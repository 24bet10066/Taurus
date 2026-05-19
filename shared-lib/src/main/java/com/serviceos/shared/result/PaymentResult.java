package com.serviceos.shared.result;

import com.serviceos.shared.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public sealed interface PaymentResult {

    record Success(
            UUID paymentId,
            UUID jobId,
            BigDecimal amount,
            PaymentMethod method,
            String gatewayRef,
            Instant receivedAt
    ) implements PaymentResult {}

    record Pending(UUID paymentId, String gatewayOrderId) implements PaymentResult {}

    record Failed(String code, String message) implements PaymentResult {}

    record Refunded(UUID paymentId, BigDecimal amount, String refundRef) implements PaymentResult {}

    record GatewayError(String message) implements PaymentResult {}
}
