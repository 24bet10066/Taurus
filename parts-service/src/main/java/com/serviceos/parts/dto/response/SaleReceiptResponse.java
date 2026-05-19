package com.serviceos.parts.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SaleReceiptResponse(
        UUID saleId,
        UUID technicianId,
        String technicianName,
        List<LineItem> items,
        BigDecimal totalAmount,
        String paymentMethod,
        BigDecimal creditUsed,
        BigDecimal newCreditBalance,
        BigDecimal availableCredit,
        Instant createdAt
) {
    public record LineItem(UUID partId, String partName, int qty, BigDecimal unitPrice, BigDecimal lineTotal) {}
}
