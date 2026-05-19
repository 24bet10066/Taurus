package com.serviceos.technician.dto.request;

import com.serviceos.shared.enums.ApplianceType;

import java.util.List;

public record UpdateTechnicianRequest(
        String name,
        String email,
        List<ApplianceType> skills,
        String city,
        String pincode,
        String status,
        Boolean approved,
        Boolean active
) {}
