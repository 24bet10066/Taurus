package com.serviceos.job.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_parts_used")
@Getter @Setter @NoArgsConstructor
public class JobPartUsed {

    @Id
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "part_id", nullable = false)
    private UUID partId;

    @Column(name = "part_name")
    private String partName;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitCost;

    /** SHOP or TECHNICIAN_CARRIED */
    @Column(name = "source", nullable = false)
    private String source = "SHOP";

    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;

    @PrePersist
    void onPersist() {
        if (id == null) id = UUID.randomUUID();
        if (addedAt == null) addedAt = Instant.now();
    }
}
