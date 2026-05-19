package com.serviceos.job.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignJobRequest(
        @NotNull UUID primaryTechId,
        UUID assistantTechId
) {}
