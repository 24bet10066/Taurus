package com.serviceos.analytics.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "daily_job_metrics")
@IdClass(DailyJobMetricsId.class)
@Getter @Setter @NoArgsConstructor
public class DailyJobMetrics {

    @Id
    @Column(name = "metric_date")
    private LocalDate metricDate;

    @Id
    @Column(name = "appliance_type")
    private String applianceType;

    @Id
    @Column(name = "area")
    private String area;

    @Column(name = "total_jobs")
    private int totalJobs = 0;

    @Column(name = "completed_jobs")
    private int completedJobs = 0;

    @Column(name = "cancelled_jobs")
    private int cancelledJobs = 0;

    @Column(name = "total_revenue", precision = 12, scale = 2)
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Column(name = "avg_job_value", precision = 10, scale = 2)
    private BigDecimal avgJobValue;

    public DailyJobMetrics(LocalDate metricDate, String applianceType, String area) {
        this.metricDate = metricDate;
        this.applianceType = applianceType;
        this.area = area;
    }
}
