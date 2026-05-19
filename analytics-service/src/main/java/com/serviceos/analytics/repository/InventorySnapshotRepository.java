package com.serviceos.analytics.repository;

import com.serviceos.analytics.entity.InventorySnapshot;
import com.serviceos.analytics.entity.InventorySnapshotId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventorySnapshotRepository extends JpaRepository<InventorySnapshot, InventorySnapshotId> {

    Optional<InventorySnapshot> findBySnapshotDateAndPartId(LocalDate date, UUID partId);

    List<InventorySnapshot> findBySnapshotDate(LocalDate date);

    @Query("SELECT MAX(s.snapshotDate) FROM InventorySnapshot s")
    Optional<LocalDate> findLatestSnapshotDate();

    long countBySnapshotDateAndStockLevelLessThanEqual(LocalDate date, int threshold);

    @Query(value = """
            SELECT s.part_id, s.part_name, SUM(s.parts_sold_this_week) as total_sold
            FROM inventory_snapshots s
            WHERE s.snapshot_date >= :from
            GROUP BY s.part_id, s.part_name
            ORDER BY total_sold DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopPartsBySalesInRange(@Param("from") LocalDate from, @Param("limit") int limit);
}
