package com.serviceos.customer.controller;

import com.serviceos.customer.dto.request.CreateApplianceRequest;
import com.serviceos.customer.dto.request.UpdateApplianceRequest;
import com.serviceos.customer.dto.request.UpdateCustomerRequest;
import com.serviceos.customer.dto.request.UpsertCustomerRequest;
import com.serviceos.customer.dto.response.ApplianceResponse;
import com.serviceos.customer.dto.response.CustomerProfileResponse;
import com.serviceos.customer.dto.response.CustomerResponse;
import com.serviceos.customer.service.CustomerService;
import com.serviceos.shared.dto.ApiResponse;
import com.serviceos.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping("/upsert")
    @Operation(summary = "Create or update customer by phone")
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN_HIRED')")
    public ResponseEntity<ApiResponse<CustomerResponse>> upsert(
            @RequestBody @Valid UpsertCustomerRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(customerService.upsert(req)));
    }

    @GetMapping
    @Operation(summary = "List customers (with optional search)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<CustomerResponse>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(customerService.list(q, page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID")
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN_HIRED')")
    public ResponseEntity<ApiResponse<CustomerResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(customerService.getById(id)));
    }

    @GetMapping("/{id}/profile")
    @Operation(summary = "Get customer profile with all appliances")
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN_HIRED')")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> getProfile(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(customerService.getProfile(id)));
    }

    @GetMapping("/phone/{phone}")
    @Operation(summary = "Lookup customer by phone number")
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN_HIRED')")
    public ResponseEntity<ApiResponse<CustomerResponse>> getByPhone(@PathVariable String phone) {
        return ResponseEntity.ok(ApiResponse.ok(customerService.getByPhone(phone)));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update customer details")
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN_HIRED')")
    public ResponseEntity<ApiResponse<CustomerResponse>> update(
            @PathVariable UUID id,
            @RequestBody UpdateCustomerRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(customerService.update(id, req)));
    }

    // -------------------------------------------------------------------------
    // Appliances
    // -------------------------------------------------------------------------

    @PostMapping("/{id}/appliances")
    @Operation(summary = "Add appliance to customer")
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN_HIRED')")
    public ResponseEntity<ApiResponse<ApplianceResponse>> addAppliance(
            @PathVariable UUID id,
            @RequestBody @Valid CreateApplianceRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(customerService.addAppliance(id, req)));
    }

    @GetMapping("/{id}/appliances")
    @Operation(summary = "List customer's appliances")
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN_HIRED')")
    public ResponseEntity<ApiResponse<List<ApplianceResponse>>> listAppliances(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(customerService.listAppliances(id)));
    }

    @PatchMapping("/{id}/appliances/{applianceId}")
    @Operation(summary = "Update appliance")
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN_HIRED')")
    public ResponseEntity<ApiResponse<ApplianceResponse>> updateAppliance(
            @PathVariable UUID id,
            @PathVariable UUID applianceId,
            @RequestBody UpdateApplianceRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(customerService.updateAppliance(id, applianceId, req)));
    }

    @DeleteMapping("/{id}/appliances/{applianceId}")
    @Operation(summary = "Remove appliance")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteAppliance(
            @PathVariable UUID id, @PathVariable UUID applianceId) {
        customerService.deleteAppliance(id, applianceId);
        return ResponseEntity.ok(ApiResponse.ok("deleted"));
    }
}
