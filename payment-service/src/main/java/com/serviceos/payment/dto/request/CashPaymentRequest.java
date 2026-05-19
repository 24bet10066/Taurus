package com.serviceos.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CashPaymentRequest(
        @NotNull UUID jobId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        UUID collectedBy,
        String notes
) {}
