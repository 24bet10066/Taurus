package com.serviceos.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record JobTrendEntry(
        LocalDate date,
        int totalJobs,
        int completedJobs,
        int cancelledJobs,
        BigDecimal revenue
) {}
