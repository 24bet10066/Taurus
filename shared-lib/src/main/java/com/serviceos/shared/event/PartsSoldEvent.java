package com.serviceos.shared.event;

import com.serviceos.shared.enums.MovementType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PartsSoldEvent(
        UUID saleId,
        UUID partId,
        String sku,
        UUID buyerId,
        String buyerType,
        UUID jobId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        MovementType movementType,
        Instant soldAt
) {
    public static final String TOPIC = "parts.sold";
}
