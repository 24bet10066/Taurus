package com.serviceos.parts.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "spare_parts")
@Getter @Setter @NoArgsConstructor
public class SparePart {

    @Id
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "sku", unique = true)
    private String sku;

    @Column(name = "category")
    private String category;

    @Column(name = "appliance_type")
    private String applianceType;

    @Column(name = "brand")
    private String brand;

    @Column(name = "is_oem", nullable = false)
    private boolean oem = false;

    @Column(name = "buy_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal buyPrice;

    @Column(name = "sell_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal sellPrice;

    /** Price charged when a hired technician takes this part from the shop for a job */
    @Column(name = "internal_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal internalPrice;

    @Column(name = "current_stock", nullable = false)
    private int currentStock = 0;

    @Column(name = "min_stock", nullable = false)
    private int minStock = 1;

    /** Rack/shelf reference e.g. "Rack-3/Shelf-B" */
    @Column(name = "location")
    private String location;

    @Column(name = "is_fast_moving", nullable = false)
    private boolean fastMoving = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "weekly_forecast", precision = 10, scale = 2)
    private BigDecimal weeklyForecast = BigDecimal.ZERO;

    @Column(name = "reorder_point")
    private int reorderPoint = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onPersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}
