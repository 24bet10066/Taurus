package com.serviceos.parts.controller;

import com.serviceos.parts.dto.request.CreditPaymentRequest;
import com.serviceos.parts.dto.request.UpdateCreditLimitRequest;
import com.serviceos.parts.dto.response.TechnicianCreditPage;
import com.serviceos.parts.security.AuthenticatedUser;
import com.serviceos.parts.service.CreditService;
import com.serviceos.shared.dto.ApiResponse;
import com.serviceos.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/parts/credit")
@Tag(name = "Credit", description = "Freelancer credit ledger")
public class CreditController {

    private final CreditService creditService;

    public CreditController(CreditService creditService) {
        this.creditService = creditService;
    }

    @GetMapping("/{technicianId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN_FREE')")
    @Operation(summary = "Get technician credit page (last 20 transactions)")
    public ResponseEntity<ApiResponse<TechnicianCreditPage>> getCreditPage(
            @PathVariable UUID technicianId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        // TECHNICIAN_FREE may only see their own page
        if (user.role() == Role.TECHNICIAN_FREE && !user.userId().equals(technicianId)) {
            throw new AccessDeniedException("You can only view your own credit page");
        }
        return ResponseEntity.ok(ApiResponse.ok(creditService.getCreditPage(technicianId)));
    }

    @PostMapping("/{technicianId}/payment")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Record cash payment by freelancer to reduce credit balance")
    public ResponseEntity<ApiResponse<TechnicianCreditPage>> recordPayment(
            @PathVariable UUID technicianId,
            @RequestBody @Valid CreditPaymentRequest req,
            @AuthenticationPrincipal AuthenticatedUser user) {
        TechnicianCreditPage page = creditService.recordPayment(technicianId, req, user.phone());
        return ResponseEntity.ok(ApiResponse.ok(page, "Payment recorded"));
    }

    @PutMapping("/{technicianId}/limit")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Set credit limit for a technician (0 = disabled, >0 = enabled)")
    public ResponseEntity<ApiResponse<TechnicianCreditPage>> setCreditLimit(
            @PathVariable UUID technicianId,
            @RequestBody @Valid UpdateCreditLimitRequest req,
            @AuthenticationPrincipal AuthenticatedUser user) {
        TechnicianCreditPage page = creditService.setCreditLimit(technicianId, req, user.phone());
        return ResponseEntity.ok(ApiResponse.ok(page, "Credit limit updated"));
    }
}
