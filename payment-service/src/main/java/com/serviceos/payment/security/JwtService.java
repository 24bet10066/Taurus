package com.serviceos.payment.security;

import com.serviceos.shared.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
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
        String roleStr = claims.get("role", String.class);
        Role role = roleStr != null ? Role.valueOf(roleStr) : null;
        return new Parsed(claims.getId(), userId, claims.get("phone", String.class), role);
    }

    public record Parsed(String jti, UUID userId, String phone, Role role) {}
}
