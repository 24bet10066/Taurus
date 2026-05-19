package com.serviceos.auth.controller;

import com.serviceos.auth.entity.BusinessConfig;
import com.serviceos.auth.service.BusinessConfigService;
import com.serviceos.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Business Config", description = "Dynamic business configuration")
public class BusinessConfigController {

    private final BusinessConfigService configService;

    public BusinessConfigController(BusinessConfigService configService) {
        this.configService = configService;
    }

    // ── Admin endpoints (via api-gateway, requires ADMIN role) ────────────────

    @GetMapping("/api/v1/config")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all config entries")
    public ResponseEntity<ApiResponse<List<BusinessConfig>>> listAll() {
        return ResponseEntity.ok(ApiResponse.ok(configService.getAll()));
    }

    @GetMapping("/api/v1/config/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get single config value")
    public ResponseEntity<ApiResponse<BusinessConfig>> get(@PathVariable String key) {
        return configService.getEntry(key)
                .map(c -> ResponseEntity.ok(ApiResponse.ok(c)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/api/v1/config/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a config value")
    public ResponseEntity<ApiResponse<BusinessConfig>> update(
            @PathVariable String key,
            @RequestBody UpdateRequest body,
            Authentication auth) {
        String caller = auth != null ? auth.getName() : "admin";
        BusinessConfig saved = configService.set(key, body.value(), caller);
        return ResponseEntity.ok(ApiResponse.ok(saved, "Config updated"));
    }

    // ── Internal endpoint (service-to-service, no JWT required) ──────────────

    @GetMapping("/internal/config/{key}")
    @Operation(summary = "Internal: get config value by key (no auth)")
    public ResponseEntity<Map<String, String>> getInternal(@PathVariable String key) {
        String value = configService.getValue(key);
        if (value == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("key", key, "value", value));
    }

    // ── DTO ───────────────────────────────────────────────────────────────────

    public record UpdateRequest(
            @NotBlank @Size(max = 1000) String value
    ) {}
}
