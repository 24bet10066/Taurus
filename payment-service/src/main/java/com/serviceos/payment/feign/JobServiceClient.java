package com.serviceos.payment.feign;

import com.serviceos.shared.dto.ApiResponse;
import com.serviceos.shared.enums.JobStatus;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(name = "job-service", url = "${services.job.url}")
public interface JobServiceClient {

    @GetMapping("/internal/jobs/{jobId}/status")
    ApiResponse<JobStatusDTO> getJobStatus(@PathVariable("jobId") UUID jobId);

    @PutMapping("/internal/jobs/{jobId}/payment-status")
    ApiResponse<String> markPaymentCollected(
            @PathVariable("jobId") UUID jobId,
            @RequestParam("status") String status,
            @RequestParam(value = "method", required = false) String method);

    record JobStatusDTO(
            UUID jobId,
            JobStatus status,
            String paymentStatus,
            BigDecimal actualCharge,
            String customerPhone) {}
}
