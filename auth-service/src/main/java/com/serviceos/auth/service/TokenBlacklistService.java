package com.serviceos.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class TokenBlacklistService {

    private static final String KEY = "jwt:revoked:%s";

    private final StringRedisTemplate redis;

    public TokenBlacklistService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void revoke(String jti, Instant expiresAt) {
        long ttl = Duration.between(Instant.now(), expiresAt).toSeconds();
        if (ttl <= 0) {
            return;
        }
        redis.opsForValue().set(KEY.formatted(jti), "1", Duration.ofSeconds(ttl));
    }

    public boolean isRevoked(String jti) {
        Boolean exists = redis.hasKey(KEY.formatted(jti));
        return Boolean.TRUE.equals(exists);
    }
}
