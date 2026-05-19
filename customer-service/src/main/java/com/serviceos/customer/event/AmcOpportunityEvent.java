package com.serviceos.customer.event;

import com.serviceos.shared.enums.ApplianceType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AmcOpportunityEvent(
        UUID customerId,
        String customerName,
        String customerPhone,
        UUID applianceId,
        ApplianceType applianceType,
        String brand,
        String reason,
        LocalDate dueDate,
        Instant publishedAt
) {
    public static final String TOPIC = "customer.amc-opportunity";

    public static final String REASON_SERVICE_DUE   = "SERVICE_DUE";
    public static final String REASON_AMC_EXPIRING   = "AMC_EXPIRING";
}
