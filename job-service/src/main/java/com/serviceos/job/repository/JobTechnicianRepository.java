package com.serviceos.job.repository;

import com.serviceos.job.entity.JobTechnician;
import com.serviceos.job.entity.JobTechnicianId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobTechnicianRepository extends JpaRepository<JobTechnician, JobTechnicianId> {

    @Query("SELECT jt FROM JobTechnician jt WHERE jt.id.jobId = :jobId")
    List<JobTechnician> findByJobId(@Param("jobId") UUID jobId);

    @Query("SELECT jt FROM JobTechnician jt WHERE jt.id.jobId = :jobId AND jt.role = :role")
    Optional<JobTechnician> findByJobIdAndRole(@Param("jobId") UUID jobId, @Param("role") String role);

    @Query("SELECT COUNT(jt) > 0 FROM JobTechnician jt WHERE jt.id.jobId = :jobId AND jt.id.technicianId = :techId")
    boolean existsByJobIdAndTechnicianId(@Param("jobId") UUID jobId, @Param("techId") UUID techId);

    @Query("SELECT jt FROM JobTechnician jt WHERE jt.id.technicianId = :techId")
    List<JobTechnician> findByTechnicianId(@Param("techId") UUID techId);

    @Modifying
    @Query("DELETE FROM JobTechnician jt WHERE jt.id.jobId = :jobId")
    void deleteByJobId(@Param("jobId") UUID jobId);
}
