package com.serviceos.analytics.consumer;

import com.serviceos.analytics.entity.DailyJobMetrics;
import com.serviceos.analytics.entity.TechnicianPerformance;
import com.serviceos.analytics.repository.DailyJobMetricsRepository;
import com.serviceos.analytics.repository.TechnicianPerformanceRepository;
import com.serviceos.shared.event.JobCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class JobCompletedConsumer {

    private static final Logger log = LoggerFactory.getLogger(JobCompletedConsumer.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final DailyJobMetricsRepository metricsRepository;
    private final TechnicianPerformanceRepository perfRepository;

    public JobCompletedConsumer(DailyJobMetricsRepository metricsRepository,
                                TechnicianPerformanceRepository perfRepository) {
        this.metricsRepository = metricsRepository;
        this.perfRepository = perfRepository;
    }

    @KafkaListener(topics = JobCompletedEvent.TOPIC, groupId = "analytics-service")
    @Transactional
    public void onJobCompleted(JobCompletedEvent event) {
        try {
            LocalDate date = event.completedAt().atZone(IST).toLocalDate();
            String applianceType = event.applianceType() != null ? event.applianceType().name() : "UNKNOWN";
            String area = event.area() != null ? event.area() : "UNKNOWN";

            upsertDailyMetrics(date, applianceType, area, event.totalAmount());
            if (event.technicianId() != null) {
                upsertTechPerformance(event, date);
            }
        } catch (Exception ex) {
            log.error("Error processing job.completed event {}: {}", event.jobId(), ex.getMessage(), ex);
        }
    }

    private void upsertDailyMetrics(LocalDate date, String applianceType, String area, BigDecimal revenue) {
        DailyJobMetrics metrics = metricsRepository
                .findById(new com.serviceos.analytics.entity.DailyJobMetricsId(date, applianceType, area))
                .orElseGet(() -> new DailyJobMetrics(date, applianceType, area));

        metrics.setTotalJobs(metrics.getTotalJobs() + 1);
        metrics.setCompletedJobs(metrics.getCompletedJobs() + 1);
        BigDecimal rev = revenue != null ? revenue : BigDecimal.ZERO;
        metrics.setTotalRevenue(metrics.getTotalRevenue().add(rev));
        if (metrics.getCompletedJobs() > 0) {
            metrics.setAvgJobValue(metrics.getTotalRevenue()
                    .divide(BigDecimal.valueOf(metrics.getCompletedJobs()), 2, java.math.RoundingMode.HALF_UP));
        }
        metricsRepository.save(metrics);
    }

    private void upsertTechPerformance(JobCompletedEvent event, LocalDate date) {
        LocalDate monthStart = date.withDayOfMonth(1);
        TechnicianPerformance perf = perfRepository
                .findByTechnicianIdAndMetricMonth(event.technicianId(), monthStart)
                .orElseGet(() -> new TechnicianPerformance(event.technicianId(), monthStart));

        perf.setTotalJobs(perf.getTotalJobs() + 1);
        perf.setCompletedJobs(perf.getCompletedJobs() + 1);
        BigDecimal rev = event.totalAmount() != null ? event.totalAmount() : BigDecimal.ZERO;
        perf.setTotalRevenue(perf.getTotalRevenue().add(rev));
        perfRepository.save(perf);
    }
}
