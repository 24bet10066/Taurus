package com.serviceos.parts.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record InternalPriceResponse(
        UUID partId,
        String name,
        String sku,
        BigDecimal sellPrice,
        BigDecimal internalPrice,
        int currentStock
) {}
