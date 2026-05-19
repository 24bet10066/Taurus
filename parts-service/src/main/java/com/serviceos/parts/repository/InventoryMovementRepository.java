package com.serviceos.parts.repository;

import com.serviceos.parts.entity.InventoryMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {

    Page<InventoryMovement> findByPartIdOrderByCreatedAtDesc(UUID partId, Pageable pageable);

    /** Sum absolute outward quantities for a part in a date range (for EMA forecast). */
    @Query("""
        SELECT COALESCE(SUM(ABS(m.quantity)), 0) FROM InventoryMovement m
        WHERE m.partId = :partId
          AND m.movementType IN ('FREELANCER_SALE', 'JOB_USE', 'DAMAGE')
          AND m.createdAt >= :from
          AND m.createdAt <  :to
        """)
    int sumOutwardQuantity(
            @Param("partId") UUID partId,
            @Param("from")   Instant from,
            @Param("to")     Instant to
    );
}
