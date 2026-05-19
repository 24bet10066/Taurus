package com.serviceos.parts.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "parts_sales")
@Getter @Setter @NoArgsConstructor
public class PartsSale {

    @Id
    private UUID id;

    @Column(name = "technician_id", nullable = false)
    private UUID technicianId;

    @Column(name = "technician_name")
    private String technicianName;

    @Convert(converter = SaleItemListConverter.class)
    @Column(name = "items", nullable = false, columnDefinition = "jsonb")
    private List<SaleItem> items;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /** CASH / CREDIT / UPI */
    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "credit_used", precision = 10, scale = 2)
    private BigDecimal creditUsed = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onPersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}
