package com.serviceos.shared.event;

import java.time.Instant;
import java.util.UUID;

public record InventoryReorderEvent(
        UUID partId,
        String sku,
        String partName,
        int currentStock,
        int reorderLevel,
        int forecastedDemand,
        int suggestedQuantity,
        Instant triggeredAt
) {
    public static final String TOPIC = "inventory.reorder-alert";
}
