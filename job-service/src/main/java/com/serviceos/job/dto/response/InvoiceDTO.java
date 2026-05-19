package com.serviceos.job.dto.response;

import com.serviceos.shared.enums.ApplianceType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InvoiceDTO(
        UUID jobId,
        String customerName,
        String customerPhone,
        ApplianceType applianceType,
        String brand,
        String issueDescription,
        String technicianName,
        BigDecimal laborCharge,
        List<PartLineItem> parts,
        BigDecimal totalCharge,
        String paymentMethod,
        Instant completedAt,
        Instant warrantyValidTill
) {
    public record PartLineItem(
            UUID partId,
            String partName,
            int quantity,
            BigDecimal unitCost,
            BigDecimal lineTotal
    ) {}
}
