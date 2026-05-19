package com.serviceos.job.entity;

import com.serviceos.shared.enums.ApplianceType;
import com.serviceos.shared.enums.JobStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@Getter @Setter @NoArgsConstructor
public class Job {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "appliance_id")
    private UUID applianceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "appliance_type")
    private ApplianceType applianceType;

    @Column(name = "brand")
    private String brand;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "issue_description", nullable = false)
    private String issueDescription;

    @Column(name = "priority", nullable = false)
    private String priority = "NORMAL";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobStatus status = JobStatus.REQUESTED;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "estimated_charge", precision = 10, scale = 2)
    private BigDecimal estimatedCharge;

    @Column(name = "actual_charge", precision = 10, scale = 2)
    private BigDecimal actualCharge;

    @Column(name = "base_charge", precision = 10, scale = 2)
    private BigDecimal baseCharge;

    @Column(name = "travel_surcharge", precision = 10, scale = 2)
    private BigDecimal travelSurcharge = BigDecimal.ZERO;

    @Column(name = "urgency_surcharge", precision = 10, scale = 2)
    private BigDecimal urgencySurcharge = BigDecimal.ZERO;

    @Column(name = "labor_charge", precision = 10, scale = 2)
    private BigDecimal laborCharge;

    @Column(name = "parts_charge", precision = 10, scale = 2)
    private BigDecimal partsCharge;

    @Column(name = "discount", precision = 10, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "final_charge", precision = 10, scale = 2)
    private BigDecimal finalCharge;

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus = "PENDING";

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "payment_ref")
    private String paymentRef;

    @Column(name = "area")
    private String area;

    @Column(name = "customer_notes", columnDefinition = "TEXT")
    private String customerNotes;

    @Column(name = "technician_notes", columnDefinition = "TEXT")
    private String technicianNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onPersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
