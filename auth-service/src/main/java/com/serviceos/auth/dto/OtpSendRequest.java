package com.serviceos.auth.dto;

import com.serviceos.auth.enums.OtpPurpose;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record OtpSendRequest(
        @Pattern(regexp = "^[0-9]{10,15}$", message = "phone must be 10-15 digits")
        String phone,
        @NotNull
        OtpPurpose purpose
) {}
