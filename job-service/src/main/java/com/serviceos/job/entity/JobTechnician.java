package com.serviceos.job.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "job_technicians")
@Getter @Setter @NoArgsConstructor
public class JobTechnician {

    @EmbeddedId
    private JobTechnicianId id;

    /** PRIMARY or ASSISTANT */
    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    public JobTechnician(JobTechnicianId id, String role) {
        this.id = id;
        this.role = role;
        this.assignedAt = Instant.now();
    }
}
