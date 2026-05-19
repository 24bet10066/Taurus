package com.serviceos.customer.dto.request;

import java.time.LocalDate;

public record UpdateApplianceRequest(
        String brand,
        String model,
        String serialNumber,
        LocalDate purchaseDate,
        LocalDate amcStartDate,
        LocalDate amcEndDate,
        LocalDate nextServiceDue,
        String notes
) {}
