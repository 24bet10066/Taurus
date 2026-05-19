package com.serviceos.analytics.dto;

import java.math.BigDecimal;

public record RevenueBreakdownResponse(
        String month,
        BigDecimal laborRevenue,
        BigDecimal partsRevenue,
        BigDecimal totalRevenue
) {}
