package com.serviceos.parts.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "credit_transactions")
@Getter @Setter @NoArgsConstructor
public class CreditTransaction {

    @Id
    private UUID id;

    @Column(name = "technician_id", nullable = false)
    private UUID technicianId;

    /** DEBIT (purchase on credit) or PAYMENT (cash paid back) */
    @Column(name = "type", nullable = false, length = 10)
    private String type;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 10, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "recorded_by")
    private String recordedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onPersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}
