package com.serviceos.shared.result;

import com.serviceos.shared.enums.Role;

import java.time.Instant;
import java.util.UUID;

public sealed interface AuthResult
        permits AuthResult.Success, AuthResult.OtpPending, AuthResult.Failure {

    record Success(
            UUID userId,
            String phone,
            Role role,
            String accessToken,
            String refreshToken,
            Instant expiresAt
    ) implements AuthResult {}

    record OtpPending(String phone, Instant expiresAt) implements AuthResult {}

    record Failure(String code, String message) implements AuthResult {}
}
