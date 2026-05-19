package com.serviceos.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TechPerformanceEntry(
        UUID technicianId,
        LocalDate metricMonth,
        int totalJobs,
        int completedJobs,
        BigDecimal totalRevenue,
        Integer avgJobMinutes
) {}
