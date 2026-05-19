package com.serviceos.auth.service;

import com.serviceos.auth.config.AuthProperties;
import com.serviceos.shared.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final AuthProperties props;
    private final SecretKey key;

    public JwtService(AuthProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.jwt().secret().getBytes(StandardCharsets.UTF_8));
    }

    public Issued issueAccessToken(UUID userId, String phone, Role role) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.jwt().accessTokenTtl());
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .id(jti)
                .subject(userId.toString())
                .issuer(props.jwt().issuer())
                .claim("phone", phone)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        return new Issued(token, jti, exp);
    }

    public String issueRefreshTokenRaw() {
        Instant now = Instant.now();
        Instant exp = now.plus(props.jwt().refreshTokenTtl());
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject("refresh")
                .issuer(props.jwt().issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Instant refreshTokenExpiry() {
        return Instant.now().plus(props.jwt().refreshTokenTtl());
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
        return new Parsed(
                claims.getId(),
                userId,
                claims.get("phone", String.class),
                role,
                claims.getExpiration().toInstant()
        );
    }

    public record Issued(String token, String jti, Instant expiresAt) {}
    public record Parsed(String jti, UUID userId, String phone, Role role, Instant expiresAt) {}
}
