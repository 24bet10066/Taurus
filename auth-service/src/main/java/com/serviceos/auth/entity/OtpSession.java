package com.serviceos.auth.entity;

import com.serviceos.auth.enums.OtpPurpose;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "otp_sessions")
public class OtpSession {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "phone", nullable = false, length = 15)
    private String phone;

    @Column(name = "otp_hash", nullable = false, length = 255)
    private String otpHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 20)
    private OtpPurpose purpose;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used", nullable = false)
    private boolean used = false;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public OtpSession() {}

    public static OtpSession create(String phone, String otpHash, OtpPurpose purpose, Instant expiresAt) {
        OtpSession s = new OtpSession();
        s.id = UUID.randomUUID();
        s.phone = phone;
        s.otpHash = otpHash;
        s.purpose = purpose;
        s.expiresAt = expiresAt;
        return s;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getPhone() { return phone; }
    public String getOtpHash() { return otpHash; }
    public OtpPurpose getPurpose() { return purpose; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isUsed() { return used; }
    public int getAttempts() { return attempts; }
    public Instant getCreatedAt() { return createdAt; }

    public void markUsed() { this.used = true; }
    public void incrementAttempts() { this.attempts++; }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isUsable() {
        return !used && !isExpired();
    }
}
