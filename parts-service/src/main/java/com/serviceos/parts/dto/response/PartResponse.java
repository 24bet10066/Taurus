package com.serviceos.parts.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PartResponse(
        UUID id,
        String name,
        String sku,
        String category,
        String applianceType,
        String brand,
        boolean oem,
        BigDecimal buyPrice,
        BigDecimal sellPrice,
        BigDecimal internalPrice,
        int currentStock,
        int minStock,
        String location,
        boolean fastMoving,
        boolean active,
        BigDecimal weeklyForecast,
        int reorderPoint,
        Instant createdAt
) {}
