package com.serviceos.shared.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreditUpdatedEvent(
        UUID technicianId,
        BigDecimal delta,
        BigDecimal newBalance,
        BigDecimal creditLimit,
        String reason,
        UUID referenceId,
        Instant updatedAt
) {
    public static final String TOPIC = "credit.updated";
}
