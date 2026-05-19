package com.serviceos.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public RefreshToken() {}

    public static RefreshToken create(UUID userId, String tokenHash, Instant expiresAt, String deviceInfo) {
        RefreshToken rt = new RefreshToken();
        rt.id = UUID.randomUUID();
        rt.userId = userId;
        rt.tokenHash = tokenHash;
        rt.expiresAt = expiresAt;
        rt.deviceInfo = deviceInfo;
        return rt;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isRevoked() { return revoked; }
    public String getDeviceInfo() { return deviceInfo; }
    public Instant getCreatedAt() { return createdAt; }

    public void revoke() { this.revoked = true; }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return !revoked && !isExpired();
    }
}
