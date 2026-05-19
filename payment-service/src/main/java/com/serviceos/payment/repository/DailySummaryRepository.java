package com.serviceos.payment.repository;

import com.serviceos.payment.entity.DailySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailySummaryRepository extends JpaRepository<DailySummary, LocalDate> {

    Optional<DailySummary> findBySummaryDate(LocalDate date);
}
