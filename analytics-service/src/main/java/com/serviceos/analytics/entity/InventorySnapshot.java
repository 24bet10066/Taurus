package com.serviceos.analytics.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "inventory_snapshots")
@IdClass(InventorySnapshotId.class)
@Getter @Setter @NoArgsConstructor
public class InventorySnapshot {

    @Id
    @Column(name = "snapshot_date")
    private LocalDate snapshotDate;

    @Id
    @Column(name = "part_id")
    private UUID partId;

    @Column(name = "part_name")
    private String partName;

    @Column(name = "stock_level")
    private int stockLevel = 0;

    @Column(name = "parts_sold_this_week")
    private int partsSoldThisWeek = 0;

    public InventorySnapshot(LocalDate snapshotDate, UUID partId) {
        this.snapshotDate = snapshotDate;
        this.partId = partId;
    }
}
