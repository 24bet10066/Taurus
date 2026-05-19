package com.serviceos.shared.result;

import com.serviceos.shared.enums.JobStatus;

import java.time.Instant;
import java.util.UUID;

public sealed interface JobTransitionResult {

    record Success(
            UUID jobId,
            JobStatus from,
            JobStatus to,
            Instant changedAt
    ) implements JobTransitionResult {}

    record InvalidTransition(JobStatus from, JobStatus to, String reason) implements JobTransitionResult {}

    record JobNotFound(UUID jobId) implements JobTransitionResult {}

    record Unauthorized(String reason) implements JobTransitionResult {}

    record TerminalState(JobStatus current) implements JobTransitionResult {}
}
