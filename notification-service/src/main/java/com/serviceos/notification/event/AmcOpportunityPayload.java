package com.serviceos.notification.event;

import com.serviceos.shared.enums.ApplianceType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AmcOpportunityPayload(
        UUID customerId,
        String customerName,
        String customerPhone,
        UUID applianceId,
        ApplianceType applianceType,
        String brand,
        String reason,
        LocalDate dueDate,
        Instant publishedAt
) {}
