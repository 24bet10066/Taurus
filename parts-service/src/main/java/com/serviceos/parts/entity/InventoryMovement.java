package com.serviceos.parts.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_movements")
@Getter @Setter @NoArgsConstructor
public class InventoryMovement {

    @Id
    private UUID id;

    @Column(name = "part_id", nullable = false)
    private UUID partId;

    /** PURCHASE / JOB_USE / FREELANCER_SALE / RETURN / ADJUSTMENT / DAMAGE / OPENING_STOCK */
    @Column(name = "movement_type", nullable = false)
    private String movementType;

    /** Positive = stock in, negative = stock out */
    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "stock_after", nullable = false)
    private int stockAfter;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_value", precision = 10, scale = 2)
    private BigDecimal totalValue;

    /** job_id or sale_id */
    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "recorded_by")
    private String recordedBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onPersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}
