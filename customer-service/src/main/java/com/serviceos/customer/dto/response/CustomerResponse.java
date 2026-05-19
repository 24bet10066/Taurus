package com.serviceos.customer.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String name,
        String phone,
        String email,
        String address,
        String city,
        String pincode,
        BigDecimal totalSpent,
        int jobCount,
        Instant lastServiceDate,
        LocalDate nextServiceDue,
        boolean active,
        Instant createdAt
) {}
