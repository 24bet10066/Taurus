package com.serviceos.parts.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreditPaymentRequest(
        @NotNull @Positive BigDecimal amount,
        @NotBlank String paymentMethod,
        String notes
) {}
