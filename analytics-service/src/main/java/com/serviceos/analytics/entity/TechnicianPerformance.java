package com.serviceos.analytics.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "technician_performance")
@IdClass(TechnicianPerformanceId.class)
@Getter @Setter @NoArgsConstructor
public class TechnicianPerformance {

    @Id
    @Column(name = "technician_id")
    private UUID technicianId;

    @Id
    @Column(name = "metric_month")
    private LocalDate metricMonth;

    @Column(name = "total_jobs")
    private int totalJobs = 0;

    @Column(name = "completed_jobs")
    private int completedJobs = 0;

    @Column(name = "total_revenue", precision = 12, scale = 2)
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Column(name = "avg_job_minutes")
    private Integer avgJobMinutes;

    public TechnicianPerformance(UUID technicianId, LocalDate metricMonth) {
        this.technicianId = technicianId;
        this.metricMonth = metricMonth;
    }
}
