package com.serviceos.analytics.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serviceos.analytics.dto.*;
import com.serviceos.analytics.entity.DailyJobMetrics;
import com.serviceos.analytics.entity.TechnicianPerformance;
import com.serviceos.analytics.repository.DailyJobMetricsRepository;
import com.serviceos.analytics.repository.InventorySnapshotRepository;
import com.serviceos.analytics.repository.TechnicianPerformanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);

    private static final String KEY_DASHBOARD = "dashboard:today";
    private static final String KEY_JOBS_7D = "dashboard:jobs:7d";
    private static final String KEY_TOP_PARTS = "dashboard:top-parts:30d";
    private static final String KEY_TECH_PERF = "dashboard:tech-performance";
    private static final String KEY_PENDING = "dashboard:pending-jobs";
    private static final String KEY_ACTIVE = "dashboard:active-jobs";
    private static final String KEY_TECHS = "dashboard:available-techs";

    private final DailyJobMetricsRepository metricsRepository;
    private final TechnicianPerformanceRepository perfRepository;
    private final InventorySnapshotRepository snapshotRepository;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Value("${services.job.url:http://localhost:8082}")
    private String jobServiceUrl;

    @Value("${services.parts.url:http://localhost:8083}")
    private String partsServiceUrl;

    @Value("${services.technician.url:http://localhost:8085}")
    private String technicianServiceUrl;

    public DashboardService(DailyJobMetricsRepository metricsRepository,
                            TechnicianPerformanceRepository perfRepository,
                            InventorySnapshotRepository snapshotRepository,
                            StringRedisTemplate redis,
                            ObjectMapper objectMapper) {
        this.metricsRepository = metricsRepository;
        this.perfRepository = perfRepository;
        this.snapshotRepository = snapshotRepository;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public DashboardResponse getDashboard() {
        String cached = redis.opsForValue().get(KEY_DASHBOARD);
        if (cached != null) {
            return deserialize(cached, DashboardResponse.class);
        }
        DashboardResponse response = buildDashboard();
        redis.opsForValue().set(KEY_DASHBOARD, serialize(response), CACHE_TTL);
        return response;
    }

    public List<JobTrendEntry> getJobTrend(int days) {
        String cacheKey = "dashboard:jobs:" + days + "d";
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            return deserialize(cached, new TypeReference<>() {});
        }
        LocalDate today = LocalDate.now(IST);
        List<JobTrendEntry> trend = buildJobTrend(today.minusDays(days - 1L), today);
        redis.opsForValue().set(cacheKey, serialize(trend), CACHE_TTL);
        return trend;
    }

    public List<TechPerformanceEntry> getTechPerformance(String month) {
        String cached = redis.opsForValue().get(KEY_TECH_PERF + ":" + month);
        if (cached != null) {
            return deserialize(cached, new TypeReference<>() {});
        }
        LocalDate monthStart = LocalDate.parse(month + "-01");
        List<TechPerformanceEntry> result = perfRepository.findByMetricMonth(monthStart).stream()
                .map(p -> new TechPerformanceEntry(p.getTechnicianId(), p.getMetricMonth(),
                        p.getTotalJobs(), p.getCompletedJobs(), p.getTotalRevenue(), p.getAvgJobMinutes()))
                .toList();
        redis.opsForValue().set(KEY_TECH_PERF + ":" + month, serialize(result), CACHE_TTL);
        return result;
    }

    public List<TopPartEntry> getTopParts(int days) {
        String cacheKey = "dashboard:top-parts:" + days + "d";
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            return deserialize(cached, new TypeReference<>() {});
        }
        LocalDate from = LocalDate.now(IST).minusDays(days);
        List<Object[]> rows = snapshotRepository.findTopPartsBySalesInRange(from, 10);
        List<TopPartEntry> result = rows.stream().map(r -> new TopPartEntry(
                UUID.fromString(r[0].toString()),
                (String) r[1],
                ((Number) r[2]).intValue()
        )).toList();
        redis.opsForValue().set(cacheKey, serialize(result), CACHE_TTL);
        return result;
    }

    public RevenueBreakdownResponse getRevenueBreakdown() {
        LocalDate today = LocalDate.now(IST);
        LocalDate monthStart = today.withDayOfMonth(1);
        String monthKey = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        List<DailyJobMetrics> monthMetrics = metricsRepository
                .findByMetricDateBetweenOrderByMetricDateAsc(monthStart, today);

        // Labor revenue: metrics where area/applianceType are NOT aggregate keys
        BigDecimal laborRevenue = monthMetrics.stream()
                .filter(m -> !"ALL".equals(m.getApplianceType()))
                .map(m -> m.getTotalRevenue() != null ? m.getTotalRevenue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Parts revenue: inferred from aggregate (ALL) minus job-attributed revenue
        BigDecimal aggregateRevenue = monthMetrics.stream()
                .filter(m -> "ALL".equals(m.getApplianceType()))
                .map(m -> m.getTotalRevenue() != null ? m.getTotalRevenue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal partsRevenue = aggregateRevenue.subtract(laborRevenue).max(BigDecimal.ZERO);
        BigDecimal total = laborRevenue.add(partsRevenue);

        return new RevenueBreakdownResponse(monthKey, laborRevenue, partsRevenue, total);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    DashboardResponse buildDashboard() {
        LocalDate today = LocalDate.now(IST);

        List<DailyJobMetrics> todayMetrics = metricsRepository.findByMetricDate(today);
        int todayJobs = todayMetrics.stream().mapToInt(DailyJobMetrics::getTotalJobs).sum();
        BigDecimal todayRevenue = todayMetrics.stream()
                .map(m -> m.getTotalRevenue() != null ? m.getTotalRevenue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int doneJobs = todayMetrics.stream().mapToInt(DailyJobMetrics::getCompletedJobs).sum();

        List<BigDecimal> weeklyRevenue = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            BigDecimal dayRev = metricsRepository.findByMetricDate(day).stream()
                    .map(m -> m.getTotalRevenue() != null ? m.getTotalRevenue() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            weeklyRevenue.add(dayRev);
        }

        LocalDate latestSnap = snapshotRepository.findLatestSnapshotDate().orElse(today);
        long lowStockCount = snapshotRepository.countBySnapshotDateAndStockLevelLessThanEqual(latestSnap, 5);

        List<Object[]> topRows = metricsRepository.findTopAppliancesByDateRange(today.minusDays(7), today);
        List<DashboardResponse.ApplianceStats> topAppliances = topRows.stream()
                .filter(r -> !"ALL".equals(r[0]))
                .limit(5)
                .map(r -> new DashboardResponse.ApplianceStats(
                        (String) r[0],
                        ((Number) r[1]).intValue(),
                        r[2] != null ? new BigDecimal(r[2].toString()) : BigDecimal.ZERO))
                .toList();

        int pendingJobs = getIntFromCache(KEY_PENDING);
        int activeJobs = getIntFromCache(KEY_ACTIVE);
        int availableTechs = getIntFromCache(KEY_TECHS);

        return new DashboardResponse(
                todayJobs, todayRevenue, pendingJobs, activeJobs, doneJobs,
                availableTechs, lowStockCount, weeklyRevenue, topAppliances);
    }

    private List<JobTrendEntry> buildJobTrend(LocalDate from, LocalDate to) {
        List<DailyJobMetrics> metrics = metricsRepository.findByMetricDateBetweenOrderByMetricDateAsc(from, to);

        Map<LocalDate, int[]> byDate = new LinkedHashMap<>();
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            byDate.put(cursor, new int[]{0, 0, 0});
            cursor = cursor.plusDays(1);
        }

        Map<LocalDate, BigDecimal> revenueByDate = new LinkedHashMap<>();
        byDate.forEach((d, ignored) -> revenueByDate.put(d, BigDecimal.ZERO));

        for (DailyJobMetrics m : metrics) {
            byDate.computeIfPresent(m.getMetricDate(), (d, arr) -> {
                arr[0] += m.getTotalJobs();
                arr[1] += m.getCompletedJobs();
                arr[2] += m.getCancelledJobs();
                return arr;
            });
            revenueByDate.merge(m.getMetricDate(),
                    m.getTotalRevenue() != null ? m.getTotalRevenue() : BigDecimal.ZERO,
                    BigDecimal::add);
        }

        return byDate.entrySet().stream().map(e -> new JobTrendEntry(
                e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2],
                revenueByDate.getOrDefault(e.getKey(), BigDecimal.ZERO)
        )).toList();
    }

    void storeLiveCounts(int pending, int active, int availableTechs) {
        redis.opsForValue().set(KEY_PENDING, String.valueOf(pending), Duration.ofHours(2));
        redis.opsForValue().set(KEY_ACTIVE, String.valueOf(active), Duration.ofHours(2));
        redis.opsForValue().set(KEY_TECHS, String.valueOf(availableTechs), Duration.ofHours(2));
        redis.delete(KEY_DASHBOARD);
    }

    private int getIntFromCache(String key) {
        String val = redis.opsForValue().get(key);
        if (val == null) return 0;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception ex) {
            log.error("Serialization error: {}", ex.getMessage());
            return "{}";
        }
    }

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            log.warn("Cache deserialization failed, recomputing: {}", ex.getMessage());
            return null;
        }
    }

    private <T> T deserialize(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            log.warn("Cache deserialization failed, recomputing: {}", ex.getMessage());
            return null;
        }
    }
}
