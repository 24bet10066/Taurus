package com.serviceos.job.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record AddPartRequest(
        @NotNull UUID partId,
        String partName,
        @Min(1) int quantity,
        @NotNull @Positive BigDecimal unitCost,
        String source
) {}
