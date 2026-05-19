package com.serviceos.parts.controller;

import com.serviceos.parts.dto.request.*;
import com.serviceos.parts.dto.response.*;
import com.serviceos.parts.security.AuthenticatedUser;
import com.serviceos.parts.service.ForecastService;
import com.serviceos.parts.service.PartsService;
import com.serviceos.parts.service.SalesService;
import com.serviceos.shared.dto.ApiResponse;
import com.serviceos.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/parts")
@Tag(name = "Parts", description = "Catalog, inventory, sales")
public class PartsController {

    private final PartsService partsService;
    private final SalesService salesService;
    private final ForecastService forecastService;

    public PartsController(PartsService partsService, SalesService salesService,
                           ForecastService forecastService) {
        this.partsService = partsService;
        this.salesService = salesService;
        this.forecastService = forecastService;
    }

    // -------------------------------------------------------------------------
    // Search
    // -------------------------------------------------------------------------

    @GetMapping("/search")
    @Operation(summary = "Trie-backed prefix search (fallback: PG full-text)")
    public ResponseEntity<ApiResponse<List<PartSearchResult>>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(partsService.search(q, limit)));
    }

    // -------------------------------------------------------------------------
    // Catalog CRUD
    // -------------------------------------------------------------------------

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List parts with optional filters")
    public ResponseEntity<ApiResponse<PageResponse<PartResponse>>> listParts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String applianceType,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                partsService.listParts(category, applianceType, brand, active, page, size)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new part")
    public ResponseEntity<ApiResponse<PartResponse>> createPart(
            @RequestBody @Valid CreatePartRequest req,
            @AuthenticationPrincipal AuthenticatedUser user) {
        PartResponse part = partsService.createPart(req, user.phone());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(part, "Part created"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update part details")
    public ResponseEntity<ApiResponse<PartResponse>> updatePart(
            @PathVariable UUID id,
            @RequestBody @Valid UpdatePartRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(partsService.updatePart(id, req)));
    }

    // -------------------------------------------------------------------------
    // Inventory
    // -------------------------------------------------------------------------

    @GetMapping("/{id}/movements")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Stock movement history for a part")
    public ResponseEntity<ApiResponse<PageResponse<MovementResponse>>> getMovements(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return ResponseEntity.ok(ApiResponse.ok(partsService.getMovements(id, page, size)));
    }

    @PostMapping("/{id}/stock-adjustment")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Manual stock adjustment (damage, return, correction)")
    public ResponseEntity<ApiResponse<MovementResponse>> adjust(
            @PathVariable UUID id,
            @RequestBody @Valid StockAdjustmentRequest req,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(partsService.adjust(id, req, user.phone()), "Stock adjusted"));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Parts at or below minimum stock level")
    public ResponseEntity<ApiResponse<List<LowStockResponse>>> getLowStock() {
        return ResponseEntity.ok(ApiResponse.ok(partsService.getLowStock()));
    }

    // -------------------------------------------------------------------------
    // B2B Sales
    // -------------------------------------------------------------------------

    @PostMapping("/sales")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Counter sale to freelancer — deducts stock and handles credit")
    public ResponseEntity<ApiResponse<SaleReceiptResponse>> sell(
            @RequestBody @Valid SellPartsRequest req,
            @AuthenticationPrincipal AuthenticatedUser user) {
        SaleReceiptResponse receipt = salesService.sell(req, user.phone());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(receipt, "Sale recorded"));
    }

    @GetMapping("/sales")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sales history with optional technician and date filter")
    public ResponseEntity<ApiResponse<PageResponse<SaleReceiptResponse>>> listSales(
            @RequestParam(required = false) UUID technicianId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(salesService.listSales(technicianId, date, page, size)));
    }

    // -------------------------------------------------------------------------
    // Forecast
    // -------------------------------------------------------------------------

    @GetMapping("/forecast")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "EMA-based next-week reorder suggestions")
    public ResponseEntity<ApiResponse<List<ForecastResponse>>> getForecast() {
        return ResponseEntity.ok(ApiResponse.ok(forecastService.getForecastSuggestions()));
    }
}
