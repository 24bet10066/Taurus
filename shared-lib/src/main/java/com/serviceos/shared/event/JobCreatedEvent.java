package com.serviceos.shared.event;

import com.serviceos.shared.enums.ApplianceType;

import java.time.Instant;
import java.util.UUID;

public record JobCreatedEvent(
        UUID jobId,
        UUID customerId,
        String customerName,
        String customerPhone,
        ApplianceType applianceType,
        String problemDescription,
        String address,
        String pincode,
        Instant createdAt,
        Boolean emergencyOverride   // null treated as false; allows bypassing SMS cooldown
) {
    public static final String TOPIC = "job.created";

    public boolean isEmergency() { return Boolean.TRUE.equals(emergencyOverride); }
}
