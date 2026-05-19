package com.serviceos.parts.controller;

import com.serviceos.parts.entity.TechnicianCredit;
import com.serviceos.parts.repository.TechnicianCreditRepository;
import com.serviceos.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * No JWT required — secured by internal network policy only.
 */
@RestController
@RequestMapping("/internal/credit")
@Tag(name = "Internal Credit", description = "Service-to-service endpoints (no auth)")
public class InternalCreditController {

    private final TechnicianCreditRepository creditRepository;

    public InternalCreditController(TechnicianCreditRepository creditRepository) {
        this.creditRepository = creditRepository;
    }

    @PutMapping("/{technicianId}/limit")
    @Operation(summary = "Update credit limit for a technician (called by technician-service after trust score recompute)")
    @Transactional
    public ResponseEntity<ApiResponse<String>> updateCreditLimit(
            @PathVariable UUID technicianId,
            @RequestParam BigDecimal limit) {
        creditRepository.findById(technicianId).ifPresent(credit -> {
            credit.setCreditLimit(limit);
            creditRepository.save(credit);
        });
        return ResponseEntity.ok(ApiResponse.ok("updated", "Credit limit updated"));
    }
}
