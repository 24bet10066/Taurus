package com.serviceos.parts.repository;

import com.serviceos.parts.entity.PartsSale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface PartsSaleRepository extends JpaRepository<PartsSale, UUID> {

    @Query("""
        SELECT s FROM PartsSale s WHERE
        (:techId IS NULL OR s.technicianId = :techId) AND
        (:from   IS NULL OR s.createdAt >= :from)     AND
        (:to     IS NULL OR s.createdAt <  :to)
        ORDER BY s.createdAt DESC
        """)
    Page<PartsSale> findByFilters(
            @Param("techId") UUID techId,
            @Param("from")   Instant from,
            @Param("to")     Instant to,
            Pageable pageable
    );
}
