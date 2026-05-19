package com.serviceos.customer.dto.response;

import com.serviceos.shared.enums.ApplianceType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ApplianceResponse(
        UUID id,
        UUID customerId,
        ApplianceType applianceType,
        String brand,
        String model,
        String serialNumber,
        LocalDate purchaseDate,
        LocalDate amcStartDate,
        LocalDate amcEndDate,
        LocalDate nextServiceDue,
        String notes,
        Instant createdAt
) {}
