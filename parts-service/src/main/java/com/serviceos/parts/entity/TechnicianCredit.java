package com.serviceos.parts.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "technician_credit")
@Getter @Setter @NoArgsConstructor
public class TechnicianCredit {

    @Id
    @Column(name = "technician_id")
    private UUID technicianId;

    @Column(name = "technician_name", nullable = false)
    private String technicianName;

    @Column(name = "technician_phone")
    private String technicianPhone;

    @Column(name = "credit_limit", nullable = false, precision = 10, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO;

    /** How much the technician currently owes the shop */
    @Column(name = "current_balance", nullable = false, precision = 10, scale = 2)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(name = "total_purchased", precision = 10, scale = 2)
    private BigDecimal totalPurchased = BigDecimal.ZERO;

    @Column(name = "total_paid", precision = 10, scale = 2)
    private BigDecimal totalPaid = BigDecimal.ZERO;

    @Column(name = "last_purchase_at")
    private Instant lastPurchaseAt;

    @Column(name = "last_payment_at")
    private Instant lastPaymentAt;

    @Column(name = "is_overdue", nullable = false)
    private boolean overdue = false;

    @Column(name = "overdue_since")
    private Instant overdueSince;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
