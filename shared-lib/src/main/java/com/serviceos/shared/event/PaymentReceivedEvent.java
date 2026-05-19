package com.serviceos.shared.event;

import com.serviceos.shared.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentReceivedEvent(
        UUID paymentId,
        UUID jobId,
        UUID customerId,
        String customerPhone,
        BigDecimal amount,
        PaymentMethod method,
        String gatewayRef,
        Instant receivedAt
) {
    public static final String TOPIC = "payment.received";
}
