package com.serviceos.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "serviceos")
public record AuthProperties(
        Jwt jwt,
        Otp otp,
        TwoFactor twofactor
) {
    public record Jwt(String secret, Duration accessTokenTtl, Duration refreshTokenTtl, String issuer) {}

    public record Otp(int length, Duration ttl, int maxAttempts, int sendRateLimitPerHour) {}

    public record TwoFactor(String baseUrl, String apiKey) {}
}
