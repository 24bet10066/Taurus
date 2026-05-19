package com.serviceos.parts.dto.response;

import java.util.UUID;

public record LowStockResponse(
        UUID partId,
        String name,
        String sku,
        String location,
        int currentStock,
        int minStock,
        int deficit
) {}
