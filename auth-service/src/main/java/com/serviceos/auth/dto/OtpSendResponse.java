package com.serviceos.auth.dto;

import java.time.Instant;

public record OtpSendResponse(
        String phone,
        Instant expiresAt,
        String message
) {}
