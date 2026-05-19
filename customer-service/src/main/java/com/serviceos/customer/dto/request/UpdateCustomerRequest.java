package com.serviceos.customer.dto.request;

public record UpdateCustomerRequest(
        String name,
        String email,
        String address,
        String city,
        String pincode
) {}
