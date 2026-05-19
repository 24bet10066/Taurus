package com.serviceos.technician.entity;

import com.serviceos.shared.enums.TechnicianType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "technicians")
@Getter @Setter @NoArgsConstructor
public class Technician {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 15, unique = true)
    private String phone;

    @Column(length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TechnicianType type;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private List<String> skills = new ArrayList<>();

    @Column(length = 50)
    private String city;

    @Column(length = 10)
    private String pincode;

    @Column(name = "active_jobs", nullable = false)
    private int activeJobs = 0;

    @Column(name = "total_jobs_completed", nullable = false)
    private int totalJobsCompleted = 0;

    @Column(name = "total_parts_purchased", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPartsPurchased = BigDecimal.ZERO;

    @Column(name = "total_parts_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPartsPaid = BigDecimal.ZERO;

    @Column(name = "parts_order_count", nullable = false)
    private int partsOrderCount = 0;

    @Column(name = "trust_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal trustScore = BigDecimal.valueOf(0.5);

    @Column(name = "payment_reliability", nullable = false, precision = 5, scale = 4)
    private BigDecimal paymentReliability = BigDecimal.valueOf(0.5);

    @Column(name = "order_frequency", nullable = false, precision = 5, scale = 4)
    private BigDecimal orderFrequency = BigDecimal.valueOf(0.5);

    @Column(name = "tenure_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal tenureScore = BigDecimal.valueOf(0.5);

    @Column(name = "volume_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal volumeScore = BigDecimal.valueOf(0.5);

    @Column(name = "credit_limit", nullable = false, precision = 10, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(name = "last_trust_computed")
    private Instant lastTrustComputed;

    @Column(nullable = false)
    private boolean approved = false;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "onboarded_at", nullable = false, updatable = false)
    private Instant onboardedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        id = UUID.randomUUID();
        createdAt = Instant.now();
        onboardedAt = Instant.now();
    }
}
