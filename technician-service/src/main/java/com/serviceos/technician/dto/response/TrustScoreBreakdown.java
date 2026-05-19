package com.serviceos.technician.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TrustScoreBreakdown(
        UUID technicianId,
        int score,
        int totalJobs,
        BigDecimal paymentReliability,
        BigDecimal orderFrequency,
        BigDecimal tenureScore,
        BigDecimal volumeScore,
        Instant computedAt
) {}
