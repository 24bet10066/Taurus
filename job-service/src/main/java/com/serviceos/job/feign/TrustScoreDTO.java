package com.serviceos.job.feign;

import java.util.UUID;

public record TrustScoreDTO(UUID technicianId, int score, int totalJobs) {}
