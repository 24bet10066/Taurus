package com.serviceos.job.dto.response;

import java.time.Instant;
import java.util.UUID;

public record JobTechnicianResponse(
        UUID technicianId,
        String role,
        Instant assignedAt
) {}
