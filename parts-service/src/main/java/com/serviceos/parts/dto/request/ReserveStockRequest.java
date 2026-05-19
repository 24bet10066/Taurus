package com.serviceos.parts.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Internal endpoint: job-service reserves stock before a technician picks it up. */
public record ReserveStockRequest(
        @NotNull UUID partId,
        @NotNull UUID jobId,
        @Min(1) int quantity
) {}
