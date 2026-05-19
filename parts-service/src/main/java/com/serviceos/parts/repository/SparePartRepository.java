package com.serviceos.parts.repository;

import com.serviceos.parts.entity.SparePart;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SparePartRepository extends JpaRepository<SparePart, UUID> {

    List<SparePart> findByActiveTrue();

    /** Dynamic list with optional filters */
    @Query("""
        SELECT p FROM SparePart p WHERE
        (:category      IS NULL OR p.category     = :category)     AND
        (:applianceType IS NULL OR p.applianceType = :applianceType) AND
        (:brand         IS NULL OR p.brand         = :brand)        AND
        (:active        IS NULL OR p.active        = :active)
        ORDER BY p.name ASC
        """)
    Page<SparePart> findByFilters(
            @Param("category")      String category,
            @Param("applianceType") String applianceType,
            @Param("brand")         String brand,
            @Param("active")        Boolean active,
            Pageable pageable
    );

    /** Full-text search fallback using PostgreSQL tsvector */
    @Query(value = """
        SELECT * FROM spare_parts
        WHERE is_active = true
          AND to_tsvector('english', name || ' ' || COALESCE(sku,'') || ' ' || COALESCE(brand,''))
              @@ plainto_tsquery('english', :query)
        ORDER BY current_stock DESC
        LIMIT :lim
        """, nativeQuery = true)
    List<SparePart> fullTextSearch(@Param("query") String query, @Param("lim") int limit);

    List<SparePart> findByCurrentStockLessThanEqualAndActiveTrue(int threshold);

    @Query("SELECT p FROM SparePart p WHERE p.active = true ORDER BY p.name ASC")
    List<SparePart> findAllActive();
}
