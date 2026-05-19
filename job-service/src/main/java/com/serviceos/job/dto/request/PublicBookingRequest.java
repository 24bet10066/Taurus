package com.serviceos.job.dto.request;

import com.serviceos.shared.enums.ApplianceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PublicBookingRequest(
        @NotBlank @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number") String customerPhone,
        String customerName,
        @NotNull ApplianceType applianceType,
        String brand,
        @NotBlank String area,
        @NotBlank String issueDescription,
        String customerNotes,
        /**
         * Marketing channel that drove this booking. Free-form short code:
         *   WA_LINK   — WhatsApp deeplink (sent via the Log Call flow)
         *   IG_BIO    — Instagram bio link
         *   IG_POST   — Instagram post / story
         *   GOOGLE    — Google search / Maps
         *   DIRECT    — typed the URL directly
         *   WEB       — unspecified web visit (default)
         * Used in analytics to show which channel actually generates bookings.
         */
        String source
) {}
