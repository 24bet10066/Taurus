package com.serviceos.shared.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public sealed interface CreditResult {

    record Granted(
            UUID technicianId,
            BigDecimal newBalance,
            BigDecimal creditLimit,
            BigDecimal availableCredit,
            Instant grantedAt
    ) implements CreditResult {}

    record Settled(
            UUID technicianId,
            BigDecimal amountSettled,
            BigDecimal newBalance,
            Instant settledAt
    ) implements CreditResult {}

    record LimitExceeded(
            UUID technicianId,
            BigDecimal requested,
            BigDecimal available,
            BigDecimal creditLimit
    ) implements CreditResult {}

    record TechnicianNotFound(UUID technicianId) implements CreditResult {}

    record Blocked(UUID technicianId, String reason) implements CreditResult {}
}
