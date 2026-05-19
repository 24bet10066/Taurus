package com.serviceos.analytics.repository;

import com.serviceos.analytics.entity.TechnicianPerformance;
import com.serviceos.analytics.entity.TechnicianPerformanceId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TechnicianPerformanceRepository extends JpaRepository<TechnicianPerformance, TechnicianPerformanceId> {

    List<TechnicianPerformance> findByMetricMonth(LocalDate metricMonth);

    Optional<TechnicianPerformance> findByTechnicianIdAndMetricMonth(UUID technicianId, LocalDate metricMonth);
}
