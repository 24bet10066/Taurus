package com.serviceos.parts.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TechnicianCreditPage(
        UUID technicianId,
        String technicianName,
        String technicianPhone,
        BigDecimal currentBalance,
        BigDecimal creditLimit,
        BigDecimal availableCredit,
        BigDecimal totalPurchased,
        BigDecimal totalPaid,
        Instant lastPurchaseAt,
        Instant lastPaymentAt,
        List<CreditTransactionResponse> recentTransactions
) {}
