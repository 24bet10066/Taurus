package com.serviceos.parts.security;

import com.serviceos.shared.enums.Role;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, String phone, Role role, String jti) {}
