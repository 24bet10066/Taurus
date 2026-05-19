package com.serviceos.parts.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StockAdjustmentRequest(
        @NotNull int quantity,
        @NotBlank String reason,
        /** ADJUSTMENT, DAMAGE, RETURN, OPENING_STOCK */
        @NotBlank String movementType
) {}
