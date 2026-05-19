package com.serviceos.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VerifyPaymentRequest(
        @NotBlank String razorpayPaymentId,
        @NotBlank String razorpayOrderId,
        @NotBlank String razorpaySignature,
        @NotNull UUID jobId
) {}
