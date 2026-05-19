package com.serviceos.analytics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serviceos.analytics.entity.DailyJobMetrics;
import com.serviceos.analytics.entity.DailyJobMetricsId;
import com.serviceos.analytics.entity.InventorySnapshot;
import com.serviceos.analytics.repository.DailyJobMetricsRepository;
import com.serviceos.analytics.repository.InventorySnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Component
public class RefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(RefreshScheduler.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final DailyJobMetricsRepository metricsRepository;
    private final InventorySnapshotRepository snapshotRepository;
    private final DashboardService dashboardService;
    private final ObjectMapper objectMapper;

    @Value("${services.job.url:http://localhost:8082}")
    private String jobServiceUrl;

    @Value("${services.parts.url:http://localhost:8083}")
    private String partsServiceUrl;

    @Value("${services.technician.url:http://localhost:8085}")
    private String technicianServiceUrl;

    public RefreshScheduler(DailyJobMetricsRepository metricsRepository,
                            InventorySnapshotRepository snapshotRepository,
                            DashboardService dashboardService,
                            ObjectMapper objectMapper) {
        this.metricsRepository = metricsRepository;
        this.snapshotRepository = snapshotRepository;
        this.dashboardService = dashboardService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void refreshDailyData() {
        log.info("Starting daily analytics refresh");
        LocalDate yesterday = LocalDate.now(IST).minusDays(1);
        LocalDate today = LocalDate.now(IST);

        try {
            refreshDailyJobMetrics(yesterday);
        } catch (Exception ex) {
            log.error("Failed to refresh daily job metrics: {}", ex.getMessage());
        }

        try {
            refreshInventorySnapshot(today);
        } catch (Exception ex) {
            log.error("Failed to refresh inventory snapshot: {}", ex.getMessage());
        }

        try {
            refreshLiveCounts(today);
        } catch (Exception ex) {
            log.error("Failed to refresh live counts: {}", ex.getMessage());
        }

        log.info("Daily analytics refresh complete");
    }

    private void refreshDailyJobMetrics(LocalDate date) {
        RestClient client = RestClient.create();
        String url = jobServiceUrl + "/internal/jobs/daily-summary?date=" + date;
        String response = client.get().uri(url).retrieve().body(String.class);
        if (response == null) return;

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode entries = root.path("data").path("entries");
            for (JsonNode entry : entries) {
                String applianceType = entry.path("applianceType").asText("UNKNOWN");
                String area = entry.path("area").asText("UNKNOWN");
                int totalJobs = entry.path("totalJobs").asInt(0);
                int completedJobs = entry.path("completedJobs").asInt(0);
                int cancelledJobs = entry.path("cancelledJobs").asInt(0);
                BigDecimal revenue = new BigDecimal(entry.path("totalRevenue").asText("0"));

                DailyJobMetrics row = metricsRepository
                        .findById(new DailyJobMetricsId(date, applianceType, area))
                        .orElseGet(() -> new DailyJobMetrics(date, applianceType, area));
                row.setTotalJobs(totalJobs);
                row.setCompletedJobs(completedJobs);
                row.setCancelledJobs(cancelledJobs);
                row.setTotalRevenue(revenue);
                if (completedJobs > 0) {
                    row.setAvgJobValue(revenue.divide(BigDecimal.valueOf(completedJobs), 2,
                            java.math.RoundingMode.HALF_UP));
                }
                metricsRepository.save(row);
            }
            log.info("Refreshed daily job metrics for {}: {} entries", date, entries.size());
        } catch (Exception ex) {
            log.error("Error parsing daily-summary response: {}", ex.getMessage());
        }
    }

    private void refreshInventorySnapshot(LocalDate today) {
        RestClient client = RestClient.create();
        String url = partsServiceUrl + "/internal/parts/snapshot";
        String response = client.get().uri(url).retrieve().body(String.class);
        if (response == null) return;

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode parts = root.path("data");
            for (JsonNode part : parts) {
                UUID partId = UUID.fromString(part.path("partId").asText());
                String partName = part.path("partName").asText();
                int stockLevel = part.path("stockLevel").asInt(0);

                InventorySnapshot snap = snapshotRepository
                        .findBySnapshotDateAndPartId(today, partId)
                        .orElseGet(() -> new InventorySnapshot(today, partId));
                snap.setPartName(partName);
                snap.setStockLevel(stockLevel);
                snapshotRepository.save(snap);
            }
            log.info("Refreshed inventory snapshot for {}: {} parts", today, parts.size());
        } catch (Exception ex) {
            log.error("Error parsing inventory snapshot response: {}", ex.getMessage());
        }
    }

    private void refreshLiveCounts(LocalDate today) {
        RestClient client = RestClient.create();

        // Job status counts
        int pending = 0, active = 0;
        try {
            String url = jobServiceUrl + "/internal/jobs/status-counts?date=" + today;
            String response = client.get().uri(url).retrieve().body(String.class);
            if (response != null) {
                JsonNode counts = objectMapper.readTree(response).path("data");
                pending = counts.path("REQUESTED").asInt(0);
                active = counts.path("ASSIGNED").asInt(0)
                        + counts.path("IN_TRANSIT").asInt(0)
                        + counts.path("AT_CUSTOMER").asInt(0)
                        + counts.path("IN_PROGRESS").asInt(0)
                        + counts.path("PARTS_NEEDED").asInt(0);
            }
        } catch (Exception ex) {
            log.warn("Could not fetch job status counts: {}", ex.getMessage());
        }

        // Available technicians
        int availableTechs = 0;
        try {
            String url = technicianServiceUrl + "/internal/technicians/count";
            String response = client.get().uri(url).retrieve().body(String.class);
            if (response != null) {
                availableTechs = Integer.parseInt(response.trim());
            }
        } catch (Exception ex) {
            log.warn("Could not fetch technician count: {}", ex.getMessage());
        }

        dashboardService.storeLiveCounts(pending, active, availableTechs);
    }
}
