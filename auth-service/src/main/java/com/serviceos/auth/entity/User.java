package com.serviceos.auth.entity;

import com.serviceos.shared.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "phone", nullable = false, unique = true, length = 15)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "name", length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private Role role;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public User() {}

    public static User create(String phone, Role role, String name) {
        User u = new User();
        u.id = UUID.randomUUID();
        u.phone = phone;
        u.role = role;
        u.name = name;
        u.active = true;
        return u;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public Role getRole() { return role; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setEmail(String email) { this.email = email; }
    public void setName(String name) { this.name = name; }
    public void setRole(Role role) { this.role = role; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setActive(boolean active) { this.active = active; }
}
