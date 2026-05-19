package com.serviceos.customer.repository;

import com.serviceos.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByPhone(String phone);

    Page<Customer> findByActiveTrue(Pageable pageable);

    @Query("""
            SELECT c FROM Customer c
            WHERE c.active = true
              AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%'))
                OR c.phone LIKE CONCAT('%', :q, '%'))
            """)
    Page<Customer> search(@Param("q") String q, Pageable pageable);
}
