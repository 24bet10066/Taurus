package com.serviceos.parts.repository;

import com.serviceos.parts.entity.CreditTransaction;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, UUID> {

    List<CreditTransaction> findByTechnicianIdOrderByCreatedAtDesc(UUID technicianId, Pageable pageable);
}
