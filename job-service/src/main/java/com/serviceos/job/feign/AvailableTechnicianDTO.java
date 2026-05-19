package com.serviceos.job.feign;

import com.serviceos.shared.enums.ApplianceType;

import java.util.List;
import java.util.UUID;

public record AvailableTechnicianDTO(
        UUID techId,
        String name,
        String phone,
        List<ApplianceType> skills,
        int activeJobCount,
        int trustScore
) {}
