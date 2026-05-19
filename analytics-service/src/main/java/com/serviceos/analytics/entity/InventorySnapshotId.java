package com.serviceos.analytics.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class InventorySnapshotId implements Serializable {

    private LocalDate snapshotDate;
    private UUID partId;

    public InventorySnapshotId() {}

    public InventorySnapshotId(LocalDate snapshotDate, UUID partId) {
        this.snapshotDate = snapshotDate;
        this.partId = partId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InventorySnapshotId that)) return false;
        return Objects.equals(snapshotDate, that.snapshotDate)
                && Objects.equals(partId, that.partId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(snapshotDate, partId);
    }
}
