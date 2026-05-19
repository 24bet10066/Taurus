package com.serviceos.technician.controller;

import com.serviceos.shared.dto.ApiResponse;
import com.serviceos.shared.dto.PageResponse;
import com.serviceos.technician.dto.request.CreateTechnicianRequest;
import com.serviceos.technician.dto.request.UpdateTechnicianRequest;
import com.serviceos.technician.dto.response.TechnicianResponse;
import com.serviceos.technician.dto.response.TrustScoreBreakdown;
import com.serviceos.technician.entity.Technician;
import com.serviceos.technician.service.TechnicianService;
import com.serviceos.technician.service.TrustScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/technicians")
@Tag(name = "Technicians")
public class TechnicianController {

    private final TechnicianService technicianService;
    private final TrustScoreService trustScoreService;

    public TechnicianController(TechnicianService technicianService,
                                 TrustScoreService trustScoreService) {
        this.technicianService = technicianService;
        this.trustScoreService = trustScoreService;
    }

    @PostMapping
    @Operation(summary = "Register a technician")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TechnicianResponse>> create(
            @RequestBody @Valid CreateTechnicianRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(technicianService.create(req)));
    }

    @GetMapping
    @Operation(summary = "List technicians")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<TechnicianResponse>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(technicianService.list(q, page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get technician by ID")
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN_HIRED')")
    public ResponseEntity<ApiResponse<TechnicianResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(technicianService.getById(id)));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update technician")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TechnicianResponse>> update(
            @PathVariable UUID id, @RequestBody UpdateTechnicianRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(technicianService.update(id, req)));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve technician for deployment")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TechnicianResponse>> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                technicianService.update(id, new UpdateTechnicianRequest(
                        null, null, null, null, null, null, true, null))));
    }

    @GetMapping("/{id}/trust-score")
    @Operation(summary = "Get trust score breakdown")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TrustScoreBreakdown>> getTrustScore(@PathVariable UUID id) {
        Technician t = technicianService.requireTechnician(id);
        var breakdown = new TrustScoreBreakdown(
                t.getId(),
                t.getTrustScore().multiply(java.math.BigDecimal.valueOf(100)).intValue(),
                t.getTotalJobsCompleted(),
                t.getPaymentReliability(), t.getOrderFrequency(),
                t.getTenureScore(), t.getVolumeScore(),
                t.getLastTrustComputed()
        );
        return ResponseEntity.ok(ApiResponse.ok(breakdown));
    }

    @PostMapping("/{id}/recompute-trust")
    @Operation(summary = "Manually trigger trust score recompute (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TrustScoreBreakdown>> recomputeTrust(@PathVariable UUID id) {
        Technician t = technicianService.requireTechnician(id);
        trustScoreService.recompute(t, true);
        return getTrustScore(id);
    }
}
