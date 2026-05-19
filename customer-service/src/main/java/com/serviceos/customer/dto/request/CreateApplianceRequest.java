package com.serviceos.customer.dto.request;

import com.serviceos.shared.enums.ApplianceType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateApplianceRequest(
        @NotNull ApplianceType applianceType,
        String brand,
        String model,
        String serialNumber,
        LocalDate purchaseDate,
        LocalDate amcStartDate,
        LocalDate amcEndDate,
        LocalDate nextServiceDue,
        String notes
) {}
