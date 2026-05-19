package com.serviceos.job.dto.request;

import com.serviceos.shared.enums.JobStatus;
import jakarta.validation.constraints.NotNull;

public record StatusTransitionRequest(
        @NotNull JobStatus status,
        String reason
) {}
