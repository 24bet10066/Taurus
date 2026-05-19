package com.serviceos.shared.event;

import com.serviceos.shared.enums.ApplianceType;
import com.serviceos.shared.enums.TechnicianType;

import java.time.Instant;
import java.util.UUID;

public record JobAssignedEvent(
        UUID jobId,
        UUID technicianId,
        String technicianPhone,
        TechnicianType technicianType,
        String customerName,
        String customerPhone,
        String area,
        ApplianceType applianceType,
        String problemDescription,
        Instant assignedAt,
        Boolean emergencyOverride   // null treated as false
) {
    public static final String TOPIC = "job.assigned";

    public boolean isEmergency() { return Boolean.TRUE.equals(emergencyOverride); }
}
