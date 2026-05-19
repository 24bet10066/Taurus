package com.serviceos.job.repository;

import com.serviceos.job.entity.Job;
import com.serviceos.shared.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    @Query("""
        SELECT j FROM Job j WHERE
        (:status IS NULL OR j.status = :status) AND
        (:area   IS NULL OR j.area   = :area)   AND
        (:from   IS NULL OR j.createdAt >= :from) AND
        (:to     IS NULL OR j.createdAt < :to)   AND
        (:phone  IS NULL OR j.customerPhone = :phone) AND
        (:techId IS NULL OR EXISTS (
            SELECT jt FROM JobTechnician jt
            WHERE jt.id.jobId = j.id AND jt.id.technicianId = :techId
        ))
        ORDER BY j.createdAt DESC
        """)
    Page<Job> findByFilters(
            @Param("status") JobStatus status,
            @Param("area")   String area,
            @Param("from")   Instant from,
            @Param("to")     Instant to,
            @Param("phone")  String customerPhone,
            @Param("techId") UUID techId,
            Pageable pageable
    );

    @Query("""
        SELECT DISTINCT j FROM Job j
        JOIN JobTechnician jt ON jt.id.jobId = j.id
        WHERE jt.id.technicianId = :technicianId
        ORDER BY j.createdAt DESC
        """)
    List<Job> findByTechnicianId(@Param("technicianId") UUID technicianId);

    @Query(value = """
        SELECT appliance_type, area,
               COUNT(*) AS total_jobs,
               SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_jobs,
               SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelled_jobs,
               COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN actual_charge ELSE 0 END), 0) AS total_revenue
        FROM jobs
        WHERE (created_at AT TIME ZONE 'Asia/Kolkata')::date = :targetDate
        GROUP BY appliance_type, area
        """, nativeQuery = true)
    List<Object[]> getDailyMetricsByApplianceAndArea(@Param("targetDate") LocalDate targetDate);

    @Query(value = """
        SELECT status, COUNT(*) AS cnt
        FROM jobs
        WHERE (created_at AT TIME ZONE 'Asia/Kolkata')::date = :targetDate
        GROUP BY status
        """, nativeQuery = true)
    List<Object[]> getStatusCountsByDate(@Param("targetDate") LocalDate targetDate);
}
