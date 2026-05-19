package com.serviceos.parts.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreditTransactionResponse(
        UUID id,
        String type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String paymentMethod,
        UUID referenceId,
        String notes,
        Instant createdAt
) {}
