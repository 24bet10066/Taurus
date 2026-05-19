package com.serviceos.analytics.controller;

import com.serviceos.analytics.dto.*;
import com.serviceos.analytics.service.DashboardService;
import com.serviceos.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Analytics", description = "Admin dashboard and reporting endpoints")
public class AnalyticsController {

    private final DashboardService dashboardService;

    public AnalyticsController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Full dashboard snapshot (cached)")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getDashboard()));
    }

    @GetMapping("/jobs/trend")
    @Operation(summary = "Daily job counts and revenue for last N days")
    public ResponseEntity<ApiResponse<List<JobTrendEntry>>> getJobTrend(
            @RequestParam(defaultValue = "7") int days) {
        int safeDays = Math.min(Math.max(days, 1), 90);
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getJobTrend(safeDays)));
    }

    @GetMapping("/technicians/performance")
    @Operation(summary = "Per-technician stats for a given month (YYYY-MM)")
    public ResponseEntity<ApiResponse<List<TechPerformanceEntry>>> getTechPerformance(
            @RequestParam String month) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getTechPerformance(month)));
    }

    @GetMapping("/parts/top")
    @Operation(summary = "Top 10 parts by sales volume in last N days")
    public ResponseEntity<ApiResponse<List<TopPartEntry>>> getTopParts(
            @RequestParam(defaultValue = "30") int days) {
        int safeDays = Math.min(Math.max(days, 1), 90);
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getTopParts(safeDays)));
    }

    @GetMapping("/revenue/breakdown")
    @Operation(summary = "Current month revenue: labor vs parts")
    public ResponseEntity<ApiResponse<RevenueBreakdownResponse>> getRevenueBreakdown() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getRevenueBreakdown()));
    }
}
