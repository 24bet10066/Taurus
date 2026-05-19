package com.serviceos.technician.dto.request;

import com.serviceos.shared.enums.ApplianceType;
import com.serviceos.shared.enums.TechnicianType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record CreateTechnicianRequest(
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "\\d{10,15}") String phone,
        String email,
        @NotNull TechnicianType type,
        @NotEmpty List<ApplianceType> skills,
        String city,
        String pincode
) {}
