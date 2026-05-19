package com.serviceos.parts.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SellPartsRequest(
        @NotNull UUID technicianId,
        String technicianName,
        @NotEmpty List<SaleItemRequest> items,
        @NotBlank String paymentMethod
) {
    public record SaleItemRequest(
            @NotNull UUID partId,
            @Min(1) int quantity
    ) {}
}
