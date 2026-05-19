package com.serviceos.auth.dto;

import java.time.Instant;

public record AccessTokenResponse(
        String accessToken,
        String refreshToken,
        Instant expiresAt
) {}
