package com.serviceos.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "business_config")
@Getter @Setter @NoArgsConstructor
public class BusinessConfig {

    @Id
    @Column(name = "key", length = 100)
    private String key;

    @Column(name = "value", nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
