package com.serviceos.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "daily_summary")
@Getter @Setter @NoArgsConstructor
public class DailySummary {

    @Id
    @Column(name = "summary_date")
    private LocalDate summaryDate;

    @Column(name = "total_revenue", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Column(name = "total_jobs", nullable = false)
    private int totalJobs = 0;

    @Column(name = "cash_revenue", nullable = false, precision = 12, scale = 2)
    private BigDecimal cashRevenue = BigDecimal.ZERO;

    @Column(name = "online_revenue", nullable = false, precision = 12, scale = 2)
    private BigDecimal onlineRevenue = BigDecimal.ZERO;

    @Column(name = "parts_revenue", nullable = false, precision = 12, scale = 2)
    private BigDecimal partsRevenue = BigDecimal.ZERO;

    @Column(name = "labor_revenue", nullable = false, precision = 12, scale = 2)
    private BigDecimal laborRevenue = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    @PreUpdate
    void onSave() {
        createdAt = Instant.now();
    }
}
