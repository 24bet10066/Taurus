package com.serviceos.shared.event;

import com.serviceos.shared.enums.ApplianceType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record JobCompletedEvent(
        UUID jobId,
        UUID customerId,
        UUID technicianId,
        BigDecimal laborCharge,
        BigDecimal partsCharge,
        BigDecimal totalAmount,
        int warrantyDays,
        Instant completedAt,
        ApplianceType applianceType,
        String area
) {
    public static final String TOPIC = "job.completed";
}
