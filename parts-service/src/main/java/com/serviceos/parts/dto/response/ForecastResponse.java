package com.serviceos.parts.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ForecastResponse(
        UUID partId,
        String name,
        String sku,
        int currentStock,
        BigDecimal weeklyForecast,
        int reorderPoint,
        int suggestedOrderQty,
        boolean needsReorder
) {}
