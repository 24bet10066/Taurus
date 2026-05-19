package com.serviceos.technician.dto.response;

import com.serviceos.shared.enums.ApplianceType;
import com.serviceos.shared.enums.TechnicianType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TechnicianResponse(
        UUID id,
        String name,
        String phone,
        String email,
        TechnicianType type,
        String status,
        List<ApplianceType> skills,
        String city,
        String pincode,
        int activeJobs,
        int totalJobsCompleted,
        BigDecimal totalPartsPurchased,
        BigDecimal totalPartsPaid,
        int trustScorePercent,
        BigDecimal creditLimit,
        boolean approved,
        boolean active,
        Instant onboardedAt,
        Instant createdAt
) {}
