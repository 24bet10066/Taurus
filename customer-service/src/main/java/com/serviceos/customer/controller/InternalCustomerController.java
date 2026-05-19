package com.serviceos.customer.controller;

import com.serviceos.customer.dto.response.CustomerResponse;
import com.serviceos.customer.service.CustomerService;
import com.serviceos.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * No JWT required — secured by internal network policy only.
 */
@RestController
@RequestMapping("/internal/customers")
@Tag(name = "Internal Customers", description = "Service-to-service endpoints (no auth)")
public class InternalCustomerController {

    private final CustomerService customerService;

    public InternalCustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID (for job-service invoice generation)")
    public ResponseEntity<ApiResponse<CustomerResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(customerService.getById(id)));
    }

    @GetMapping("/by-phone/{phone}")
    @Operation(summary = "Lookup customer by phone (for booking flow)")
    public ResponseEntity<ApiResponse<CustomerResponse>> getByPhone(@PathVariable String phone) {
        return ResponseEntity.ok(ApiResponse.ok(customerService.getByPhone(phone)));
    }
}
