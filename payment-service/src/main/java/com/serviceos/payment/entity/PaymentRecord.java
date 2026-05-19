package com.serviceos.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_records")
@Getter @Setter @NoArgsConstructor
public class PaymentRecord {

    @Id
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod;

    @Column(name = "razorpay_payment_id", length = 100)
    private String razorpayPaymentId;

    @Column(name = "razorpay_order_id", length = 100)
    private String razorpayOrderId;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "collected_by")
    private UUID collectedBy;

    @Column(name = "customer_phone", length = 15)
    private String customerPhone;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    void prePersist() {
        id = UUID.randomUUID();
        createdAt = Instant.now();
    }
}
