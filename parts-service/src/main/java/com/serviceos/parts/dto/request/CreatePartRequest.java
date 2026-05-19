package com.serviceos.parts.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreatePartRequest(
        @NotBlank String name,
        String sku,
        String category,
        String applianceType,
        String brand,
        boolean oem,
        @NotNull @Positive BigDecimal buyPrice,
        @NotNull @Positive BigDecimal sellPrice,
        @NotNull @Positive BigDecimal internalPrice,
        @Min(0) int currentStock,
        @Min(0) int minStock,
        String location,
        boolean fastMoving
) {}
