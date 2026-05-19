package com.serviceos.auth.dto;

import com.serviceos.shared.enums.Role;

import java.time.Instant;
import java.util.UUID;

public record MeResponse(
        UUID userId,
        String phone,
        String name,
        String email,
        Role role,
        boolean active,
        Instant createdAt
) {}
