package com.serviceos.customer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpsertCustomerRequest(
        @NotBlank @Pattern(regexp = "\\d{10,15}") String phone,
        @NotBlank String name,
        String email,
        String address,
        String city,
        String pincode
) {}
