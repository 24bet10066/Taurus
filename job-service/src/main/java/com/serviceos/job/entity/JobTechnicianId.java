package com.serviceos.job.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class JobTechnicianId implements Serializable {

    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "technician_id")
    private UUID technicianId;
}
