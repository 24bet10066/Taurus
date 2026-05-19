package com.serviceos.job.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_status_history")
@Getter @Setter @NoArgsConstructor
public class JobStatusHistory {

    @Id
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "from_status")
    private String fromStatus;

    @Column(name = "to_status", nullable = false)
    private String toStatus;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    @PrePersist
    void onPersist() {
        if (id == null) id = UUID.randomUUID();
        if (changedAt == null) changedAt = Instant.now();
    }
}
