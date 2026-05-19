package com.serviceos.technician.repository;

import com.serviceos.technician.entity.Technician;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TechnicianRepository extends JpaRepository<Technician, UUID> {

    Optional<Technician> findByPhone(String phone);

    Page<Technician> findByActiveTrue(Pageable pageable);

    @Query("""
            SELECT t FROM Technician t
            WHERE t.active = true
              AND (LOWER(t.name) LIKE LOWER(CONCAT('%', :q, '%'))
                OR t.phone LIKE CONCAT('%', :q, '%'))
            """)
    Page<Technician> search(@Param("q") String q, Pageable pageable);

    // Find technicians with a given skill (JSON array contains check via native query)
    @Query(value = "SELECT * FROM technicians WHERE active = true AND jsonb_exists(skills::jsonb, :skill)",
           nativeQuery = true)
    List<Technician> findActiveBySkill(@Param("skill") String skill);

    List<Technician> findByTypeAndActiveTrue(com.serviceos.shared.enums.TechnicianType type);
}
