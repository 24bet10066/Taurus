package com.serviceos.parts.repository;

import com.serviceos.parts.entity.TechnicianCredit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TechnicianCreditRepository extends JpaRepository<TechnicianCredit, UUID> {}
