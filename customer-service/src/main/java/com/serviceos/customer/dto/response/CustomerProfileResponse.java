package com.serviceos.customer.dto.response;

import java.util.List;

public record CustomerProfileResponse(
        CustomerResponse customer,
        List<ApplianceResponse> appliances
) {}
