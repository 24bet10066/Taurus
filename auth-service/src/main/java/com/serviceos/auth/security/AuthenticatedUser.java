package com.serviceos.auth.security;

import com.serviceos.shared.enums.Role;

import java.time.Instant;
import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        String phone,
        Role role,
        String jti,
        Instant expiresAt
) {}
