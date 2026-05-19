package com.serviceos.analytics.repository;

import com.serviceos.analytics.entity.DailyJobMetrics;
import com.serviceos.analytics.entity.DailyJobMetricsId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DailyJobMetricsRepository extends JpaRepository<DailyJobMetrics, DailyJobMetricsId> {

    List<DailyJobMetrics> findByMetricDate(LocalDate date);

    List<DailyJobMetrics> findByMetricDateBetweenOrderByMetricDateAsc(LocalDate from, LocalDate to);

    @Query("""
            SELECT d.applianceType, SUM(d.totalJobs), SUM(d.totalRevenue)
            FROM DailyJobMetrics d
            WHERE d.metricDate >= :from AND d.metricDate <= :to
            GROUP BY d.applianceType
            ORDER BY SUM(d.totalJobs) DESC
            """)
    List<Object[]> findTopAppliancesByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            SELECT d.applianceType,
                   SUM(CASE WHEN d.applianceType = 'LABOR' THEN d.totalRevenue ELSE 0 END),
                   SUM(d.totalRevenue)
            FROM DailyJobMetrics d
            WHERE d.metricDate >= :from AND d.metricDate <= :to
            GROUP BY d.applianceType
            """)
    List<Object[]> findRevenueByMonthRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
