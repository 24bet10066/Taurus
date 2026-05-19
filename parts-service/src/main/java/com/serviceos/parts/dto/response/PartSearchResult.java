package com.serviceos.parts.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record PartSearchResult(
        UUID partId,
        String name,
        String sku,
        String brand,
        int currentStock,
        BigDecimal sellPrice,
        String location
) {}
