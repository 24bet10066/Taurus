package com.serviceos.payment.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentStatusResponse(
        UUID paymentId,
        UUID jobId,
        BigDecimal amount,
        String paymentMethod,
        String status,
        String razorpayOrderId,
        String razorpayPaymentId,
        Instant createdAt,
        Instant completedAt
) {}
