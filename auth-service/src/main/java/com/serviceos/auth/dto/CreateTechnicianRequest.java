package com.serviceos.auth.dto;

import com.serviceos.shared.enums.Role;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateTechnicianRequest(
        @Pattern(regexp = "^[0-9]{10,15}$", message = "phone must be 10-15 digits")
        String phone,
        @NotBlank
        String name,
        @Email
        String email,
        @NotNull
        Role role
) {
    @AssertTrue(message = "role must be TECHNICIAN_HIRED or TECHNICIAN_FREE")
    public boolean isTechnicianRole() {
        return role == Role.TECHNICIAN_HIRED || role == Role.TECHNICIAN_FREE;
    }
}
