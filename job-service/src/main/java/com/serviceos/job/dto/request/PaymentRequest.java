package com.serviceos.job.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotBlank String paymentMethod,
        String paymentRef,
        @NotNull @Positive BigDecimal amount
) {}
