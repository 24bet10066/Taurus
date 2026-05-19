package com.serviceos.job.dto.request;

import com.serviceos.shared.enums.ApplianceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateJobRequest(
        @NotNull UUID customerId,
        String customerName,
        String customerPhone,
        UUID applianceId,
        ApplianceType applianceType,
        String brand,
        @NotBlank String source,
        @NotBlank String issueDescription,
        String priority,
        String area,
        String customerNotes,
        BigDecimal estimatedCharge
) {}
