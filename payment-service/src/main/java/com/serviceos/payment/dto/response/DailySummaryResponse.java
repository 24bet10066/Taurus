package com.serviceos.payment.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailySummaryResponse(
        LocalDate summaryDate,
        BigDecimal totalRevenue,
        int totalJobs,
        BigDecimal cashRevenue,
        BigDecimal onlineRevenue,
        BigDecimal partsRevenue,
        BigDecimal laborRevenue
) {}
