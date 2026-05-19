package com.serviceos.parts.controller;

import com.serviceos.parts.dto.request.ReserveStockRequest;
import com.serviceos.parts.dto.response.InternalPriceResponse;
import com.serviceos.parts.repository.SparePartRepository;
import com.serviceos.parts.service.InventoryMovementService;
import com.serviceos.parts.service.PartsService;
import com.serviceos.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Internal endpoints consumed by job-service.
 * No JWT required — secured by internal network policy only.
 */
@RestController
@RequestMapping("/internal/parts")
@Tag(name = "Internal Parts", description = "Service-to-service endpoints (no auth)")
public class InternalPartsController {

    private final PartsService partsService;
    private final InventoryMovementService movementService;
    private final SparePartRepository sparePartRepository;

    public InternalPartsController(PartsService partsService,
                                   InventoryMovementService movementService,
                                   SparePartRepository sparePartRepository) {
        this.partsService = partsService;
        this.movementService = movementService;
        this.sparePartRepository = sparePartRepository;
    }

    @GetMapping("/{id}/price")
    @Operation(summary = "Get sell and internal price for a part (called by job-service)")
    public ResponseEntity<ApiResponse<InternalPriceResponse>> getPrice(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(partsService.getInternalPrice(id)));
    }

    @PostMapping("/reserve")
    @Operation(summary = "Reserve stock for a job (deducts from stock)")
    public ResponseEntity<ApiResponse<String>> reserve(@RequestBody @Valid ReserveStockRequest req) {
        // Stock is deducted immediately on reservation; type = JOB_USE
        movementService.recordMovement(
                req.partId(), "JOB_USE", -req.quantity(),
                partsService.requirePart(req.partId()).getInternalPrice(),
                req.jobId(), "job-service", "Reserved for job " + req.jobId()
        );
        return ResponseEntity.ok(ApiResponse.ok("reserved", "Stock reserved for job"));
    }

    @GetMapping("/snapshot")
    @Operation(summary = "Current inventory snapshot — all active parts with stock level (called by analytics-service)")
    public ResponseEntity<ApiResponse<List<PartSnapshotDTO>>> getSnapshot() {
        List<PartSnapshotDTO> snapshot = sparePartRepository.findAllActive().stream()
                .map(p -> new PartSnapshotDTO(p.getId(), p.getName(), p.getCurrentStock()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(snapshot));
    }

    public record PartSnapshotDTO(java.util.UUID partId, String partName, int stockLevel) {}
}
