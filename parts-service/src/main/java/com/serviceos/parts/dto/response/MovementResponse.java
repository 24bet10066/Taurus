package com.serviceos.parts.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MovementResponse(
        UUID id,
        UUID partId,
        String movementType,
        int quantity,
        int stockAfter,
        BigDecimal unitPrice,
        BigDecimal totalValue,
        UUID referenceId,
        String recordedBy,
        String notes,
        Instant createdAt
) {}
