package com.serviceos.customer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "customers")
@Getter @Setter @NoArgsConstructor
public class Customer {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 15, unique = true)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 50)
    private String city;

    @Column(length = 10)
    private String pincode;

    @Column(name = "total_spent", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalSpent = BigDecimal.ZERO;

    @Column(name = "job_count", nullable = false)
    private int jobCount = 0;

    @Column(name = "last_service_date")
    private Instant lastServiceDate;

    @Column(name = "next_service_due")
    private LocalDate nextServiceDue;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        id = UUID.randomUUID();
        createdAt = Instant.now();
    }
}
