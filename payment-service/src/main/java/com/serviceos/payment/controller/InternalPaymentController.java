package com.serviceos.payment.controller;

import com.serviceos.payment.dto.response.PaymentStatusResponse;
import com.serviceos.payment.service.PaymentService;
import com.serviceos.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * No JWT required — secured by internal network policy only.
 */
@RestController
@RequestMapping("/internal/payments")
@Tag(name = "Internal Payments", description = "Service-to-service endpoints (no auth)")
public class InternalPaymentController {

    private final PaymentService paymentService;

    public InternalPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/{jobId}/status")
    @Operation(summary = "Get payment status for a job (called by job-service)")
    public ResponseEntity<ApiResponse<List<PaymentStatusResponse>>> getStatus(
            @PathVariable UUID jobId) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getByJobId(jobId)));
    }
}
