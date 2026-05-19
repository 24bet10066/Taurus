package com.serviceos.technician.controller;

import com.serviceos.shared.dto.ApiResponse;
import com.serviceos.shared.enums.ApplianceType;
import com.serviceos.technician.entity.Technician;
import com.serviceos.technician.service.TechnicianService;
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
@RequestMapping("/internal/technicians")
@Tag(name = "Internal Technicians", description = "Service-to-service endpoints (no auth)")
public class InternalTechnicianController {

    private final TechnicianService technicianService;

    public InternalTechnicianController(TechnicianService technicianService) {
        this.technicianService = technicianService;
    }

    @GetMapping("/available")
    @Operation(summary = "Get available technicians by appliance type (called by job-service)")
    public ResponseEntity<List<AvailableTechnicianResponse>> getAvailable(
            @RequestParam("applianceType") String applianceType) {
        List<Technician> techs = technicianService.findAvailableBySkill(applianceType);
        List<AvailableTechnicianResponse> result = techs.stream()
                .filter(Technician::isApproved)
                .map(t -> {
                    List<ApplianceType> skills = t.getSkills().stream()
                            .map(ApplianceType::valueOf).toList();
                    return new AvailableTechnicianResponse(
                            t.getId(), t.getName(), t.getPhone(), skills,
                            t.getActiveJobs(),
                            t.getTrustScore().multiply(java.math.BigDecimal.valueOf(100)).intValue()
                    );
                }).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/trust-score")
    @Operation(summary = "Get trust score for a technician (called by job-service)")
    public ResponseEntity<TrustScoreResponse> getTrustScore(@PathVariable UUID id) {
        Technician t = technicianService.requireTechnician(id);
        return ResponseEntity.ok(new TrustScoreResponse(
                t.getId(),
                t.getTrustScore().multiply(java.math.BigDecimal.valueOf(100)).intValue(),
                t.getTotalJobsCompleted()
        ));
    }

    @PutMapping("/{id}/active-jobs")
    @Operation(summary = "Increment or decrement active job count (called by job-service)")
    public ResponseEntity<Void> adjustActiveJobs(
            @PathVariable UUID id, @RequestParam("delta") int delta) {
        technicianService.adjustActiveJobs(id, delta);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get technician details (called by job-service for invoicing)")
    public ResponseEntity<TechnicianDetailResponse> getDetail(@PathVariable UUID id) {
        Technician t = technicianService.requireTechnician(id);
        return ResponseEntity.ok(new TechnicianDetailResponse(t.getId(), t.getName(), t.getPhone()));
    }

    @GetMapping("/count")
    @Operation(summary = "Count active approved technicians (called by analytics-service)")
    public ResponseEntity<Integer> countActive() {
        return ResponseEntity.ok(technicianService.countActiveApproved());
    }

    // -------------------------------------------------------------------------
    // Local response types matching job-service Feign client expectations
    // -------------------------------------------------------------------------

    public record AvailableTechnicianResponse(
            UUID techId,
            String name,
            String phone,
            List<ApplianceType> skills,
            int activeJobCount,
            int trustScore) {}

    public record TrustScoreResponse(UUID technicianId, int score, int totalJobs) {}

    public record TechnicianDetailResponse(UUID id, String name, String phone) {}
}
