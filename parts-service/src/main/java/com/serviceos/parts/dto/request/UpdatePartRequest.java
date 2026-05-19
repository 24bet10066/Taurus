package com.serviceos.parts.dto.request;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UpdatePartRequest(
        String name,
        String category,
        String applianceType,
        String brand,
        Boolean oem,
        @Positive BigDecimal buyPrice,
        @Positive BigDecimal sellPrice,
        @Positive BigDecimal internalPrice,
        Integer minStock,
        String location,
        Boolean fastMoving,
        Boolean active
) {}
