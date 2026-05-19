package com.serviceos.job.dto.response;

import com.serviceos.shared.enums.ApplianceType;
import com.serviceos.shared.enums.JobStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JobResponse(
        UUID id,
        UUID customerId,
        String customerName,
        String customerPhone,
        UUID applianceId,
        ApplianceType applianceType,
        String brand,
        String source,
        String issueDescription,
        String priority,
        JobStatus status,
        Instant assignedAt,
        Instant completedAt,
        Instant cancelledAt,
        BigDecimal estimatedCharge,
        BigDecimal actualCharge,
        BigDecimal laborCharge,
        BigDecimal partsCharge,
        String paymentStatus,
        String paymentMethod,
        String area,
        String customerNotes,
        String technicianNotes,
        List<JobTechnicianResponse> technicians,
        Instant createdAt,
        Instant updatedAt
) {}
