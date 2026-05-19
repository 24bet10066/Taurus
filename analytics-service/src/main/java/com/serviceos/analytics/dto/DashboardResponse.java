package com.serviceos.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        int todayJobs,
        BigDecimal todayRevenue,
        int pendingJobs,
        int activeJobs,
        int doneJobs,
        int availableTechs,
        long lowStockCount,
        List<BigDecimal> weeklyRevenue,
        List<ApplianceStats> topAppliances
) {
    public record ApplianceStats(String applianceType, int jobCount, BigDecimal revenue) {}
}
