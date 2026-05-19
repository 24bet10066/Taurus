package com.serviceos.shared.event;

import com.serviceos.shared.enums.ApplianceType;
import com.serviceos.shared.enums.JobStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record JobStatusChangedEvent(
        UUID jobId,
        UUID customerId,
        UUID technicianId,
        JobStatus from,
        JobStatus to,
        String note,
        Instant changedAt,
        // Notification context — may be null for legacy events
        String customerPhone,
        String techName,
        String techPhone,
        ApplianceType applianceType,
        BigDecimal totalCharge,
        Boolean emergencyOverride   // null treated as false
) {
    public static final String TOPIC = "job.status.changed";

    public boolean isEmergency() { return Boolean.TRUE.equals(emergencyOverride); }
}
