package com.serviceos.job.controller;

import com.serviceos.job.entity.Job;
import com.serviceos.job.repository.JobRepository;
import com.serviceos.shared.dto.ApiResponse;
import com.serviceos.shared.enums.JobStatus;
import com.serviceos.shared.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * No JWT required — secured by internal network policy only.
 */
@RestController
@RequestMapping("/internal/jobs")
@Tag(name = "Internal Jobs", description = "Service-to-service endpoints (no auth)")
public class InternalJobController {

    private final JobRepository jobRepository;

    public InternalJobController(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @GetMapping("/{jobId}/status")
    @Operation(summary = "Get job status for payment validation")
    public ResponseEntity<ApiResponse<JobStatusDTO>> getStatus(@PathVariable UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));
        return ResponseEntity.ok(ApiResponse.ok(
                new JobStatusDTO(job.getId(), job.getStatus(), job.getPaymentStatus(),
                        job.getActualCharge(), job.getCustomerPhone())));
    }

    @PutMapping("/{jobId}/payment-status")
    @Operation(summary = "Mark job payment as collected (called by payment-service)")
    public ResponseEntity<ApiResponse<String>> markPaymentCollected(
            @PathVariable UUID jobId,
            @RequestParam String status,
            @RequestParam(required = false) String method) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));
        job.setPaymentStatus(status);
        if (method != null) job.setPaymentMethod(method);
        jobRepository.save(job);
        return ResponseEntity.ok(ApiResponse.ok("updated"));
    }

    @GetMapping("/daily-summary")
    @Operation(summary = "Daily job metrics grouped by appliance type and area (called by analytics-service)")
    public ResponseEntity<ApiResponse<DailySummaryResponse>> getDailySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<Object[]> rows = jobRepository.getDailyMetricsByApplianceAndArea(date);
        List<DailySummaryEntry> entries = rows.stream().map(r -> new DailySummaryEntry(
                (String) r[0], (String) r[1],
                ((Number) r[2]).intValue(), ((Number) r[3]).intValue(),
                ((Number) r[4]).intValue(),
                r[5] != null ? new BigDecimal(r[5].toString()) : BigDecimal.ZERO
        )).toList();
        return ResponseEntity.ok(ApiResponse.ok(new DailySummaryResponse(date, entries)));
    }

    @GetMapping("/status-counts")
    @Operation(summary = "Job counts by status for a given date (called by analytics-service)")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getStatusCounts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<Object[]> rows = jobRepository.getStatusCountsByDate(date);
        Map<String, Integer> counts = new HashMap<>();
        rows.forEach(r -> counts.put((String) r[0], ((Number) r[1]).intValue()));
        return ResponseEntity.ok(ApiResponse.ok(counts));
    }

    public record JobStatusDTO(
            UUID jobId,
            JobStatus status,
            String paymentStatus,
            BigDecimal actualCharge,
            String customerPhone) {}

    public record DailySummaryEntry(
            String applianceType, String area,
            int totalJobs, int completedJobs, int cancelledJobs,
            BigDecimal totalRevenue) {}

    public record DailySummaryResponse(LocalDate date, List<DailySummaryEntry> entries) {}
}
