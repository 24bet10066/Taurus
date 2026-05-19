package com.serviceos.payment.controller;

import com.serviceos.payment.dto.request.CashPaymentRequest;
import com.serviceos.payment.dto.request.CreateOrderRequest;
import com.serviceos.payment.dto.request.VerifyPaymentRequest;
import com.serviceos.payment.dto.response.CreateOrderResponse;
import com.serviceos.payment.dto.response.DailySummaryResponse;
import com.serviceos.payment.dto.response.PaymentStatusResponse;
import com.serviceos.payment.security.AuthenticatedUser;
import com.serviceos.payment.service.DailySummaryService;
import com.serviceos.payment.service.PaymentService;
import com.serviceos.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final DailySummaryService summaryService;

    public PaymentController(PaymentService paymentService,
                              DailySummaryService summaryService) {
        this.paymentService = paymentService;
        this.summaryService = summaryService;
    }

    @PostMapping("/cash")
    @Operation(summary = "Record a cash payment for a completed job")
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN_HIRED')")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> collectCash(
            @RequestBody @Valid CashPaymentRequest req,
            @AuthenticationPrincipal AuthenticatedUser user) {
        UUID collectedBy = user != null ? user.userId() : null;
        return ResponseEntity.ok(ApiResponse.ok(paymentService.collectCash(req, collectedBy),
                "Payment recorded"));
    }

    @PostMapping("/online/create-order")
    @Operation(summary = "Create a Razorpay order for online payment")
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN_HIRED','CUSTOMER')")
    public ResponseEntity<ApiResponse<CreateOrderResponse>> createOrder(
            @RequestBody @Valid CreateOrderRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.createRazorpayOrder(req)));
    }

    @PostMapping("/online/verify")
    @Operation(summary = "Verify Razorpay payment signature and capture")
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN_HIRED','CUSTOMER')")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> verifyPayment(
            @RequestBody @Valid VerifyPaymentRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.verifyAndCapture(req),
                "Payment verified and captured"));
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Get payment status for a job")
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN_HIRED')")
    public ResponseEntity<ApiResponse<List<PaymentStatusResponse>>> getByJob(
            @PathVariable UUID jobId) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getByJobId(jobId)));
    }

    @GetMapping("/daily-summary")
    @Operation(summary = "Get daily revenue summary (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DailySummaryResponse>> getDailySummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.ok(summaryService.getSummary(target)));
    }
}
