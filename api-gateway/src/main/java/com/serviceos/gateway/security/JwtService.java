package com.serviceos.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${serviceos.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Parsed parse(String token) throws JwtException {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        UUID userId = claims.getSubject() != null ? UUID.fromString(claims.getSubject()) : null;
        String role = claims.get("role", String.class);
        return new Parsed(userId, role);
    }

    public record Parsed(UUID userId, String role) {}
}
