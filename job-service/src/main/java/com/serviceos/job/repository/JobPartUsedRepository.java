package com.serviceos.job.repository;

import com.serviceos.job.entity.JobPartUsed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface JobPartUsedRepository extends JpaRepository<JobPartUsed, UUID> {

    List<JobPartUsed> findByJobId(UUID jobId);

    @Query("SELECT COALESCE(SUM(p.quantity * p.unitCost), 0) FROM JobPartUsed p WHERE p.jobId = :jobId")
    BigDecimal sumPartsChargeByJobId(@Param("jobId") UUID jobId);
}
