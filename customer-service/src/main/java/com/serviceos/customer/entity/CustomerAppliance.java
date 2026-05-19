package com.serviceos.customer.entity;

import com.serviceos.shared.enums.ApplianceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "customer_appliances")
@Getter @Setter @NoArgsConstructor
public class CustomerAppliance {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "appliance_type", nullable = false, length = 30)
    private ApplianceType applianceType;

    @Column(length = 50)
    private String brand;

    @Column(length = 100)
    private String model;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "amc_start_date")
    private LocalDate amcStartDate;

    @Column(name = "amc_end_date")
    private LocalDate amcEndDate;

    @Column(name = "next_service_due")
    private LocalDate nextServiceDue;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        id = UUID.randomUUID();
        createdAt = Instant.now();
    }
}
