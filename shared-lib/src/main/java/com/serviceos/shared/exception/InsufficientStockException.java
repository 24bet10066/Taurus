package com.serviceos.shared.exception;

import java.util.UUID;

public class InsufficientStockException extends RuntimeException {

    private final UUID partId;
    private final String sku;
    private final int requested;
    private final int available;

    public InsufficientStockException(UUID partId, String sku, int requested, int available) {
        super("Insufficient stock for SKU %s: requested %d, available %d".formatted(sku, requested, available));
        this.partId = partId;
        this.sku = sku;
        this.requested = requested;
        this.available = available;
    }

    public UUID getPartId() {
        return partId;
    }

    public String getSku() {
        return sku;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
