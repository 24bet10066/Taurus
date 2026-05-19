package com.serviceos.auth.dto;

import com.serviceos.shared.enums.Role;

import java.time.Instant;
import java.util.UUID;

public record TokenResponse(
        UUID userId,
        String phone,
        Role role,
        String accessToken,
        String refreshToken,
        Instant expiresAt
) {}
