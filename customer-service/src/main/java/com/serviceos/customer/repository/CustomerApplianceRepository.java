package com.serviceos.customer.repository;

import com.serviceos.customer.entity.CustomerAppliance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerApplianceRepository extends JpaRepository<CustomerAppliance, UUID> {

    List<CustomerAppliance> findByCustomerId(UUID customerId);

    Optional<CustomerAppliance> findByIdAndCustomerId(UUID id, UUID customerId);

    @Query("""
            SELECT a FROM CustomerAppliance a
            WHERE a.nextServiceDue BETWEEN :start AND :end
            """)
    List<CustomerAppliance> findServiceDueInRange(@Param("start") LocalDate start,
                                                   @Param("end") LocalDate end);

    @Query("""
            SELECT a FROM CustomerAppliance a
            WHERE a.amcEndDate BETWEEN :start AND :end
            """)
    List<CustomerAppliance> findAmcExpiringInRange(@Param("start") LocalDate start,
                                                    @Param("end") LocalDate end);
}
