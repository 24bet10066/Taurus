package com.serviceos.analytics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.serviceos.analytics.dto.DashboardResponse;
import com.serviceos.analytics.dto.JobTrendEntry;
import com.serviceos.analytics.entity.DailyJobMetrics;
import com.serviceos.analytics.entity.InventorySnapshot;
import com.serviceos.analytics.repository.DailyJobMetricsRepository;
import com.serviceos.analytics.repository.InventorySnapshotRepository;
import com.serviceos.analytics.repository.TechnicianPerformanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardServiceTest {

    @Mock DailyJobMetricsRepository metricsRepository;
    @Mock TechnicianPerformanceRepository perfRepository;
    @Mock InventorySnapshotRepository snapshotRepository;
    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> valueOps;

    @InjectMocks DashboardService dashboardService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);
        doNothing().when(valueOps).set(anyString(), anyString(), any());
        doNothing().when(valueOps).set(anyString(), anyString());
        when(snapshotRepository.findLatestSnapshotDate()).thenReturn(Optional.of(LocalDate.now()));
        when(snapshotRepository.countBySnapshotDateAndStockLevelLessThanEqual(any(), anyInt())).thenReturn(3L);
        when(metricsRepository.findTopAppliancesByDateRange(any(), any())).thenReturn(List.of());

        // Inject objectMapper via reflection (field injection doesn't work well here)
        try {
            var field = DashboardService.class.getDeclaredField("objectMapper");
            field.setAccessible(true);
            field.set(dashboardService, objectMapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void buildDashboard_noData_returnsZeros() {
        when(metricsRepository.findByMetricDate(any())).thenReturn(List.of());

        DashboardResponse resp = dashboardService.buildDashboard();

        assertThat(resp.todayJobs()).isZero();
        assertThat(resp.todayRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resp.doneJobs()).isZero();
        assertThat(resp.lowStockCount()).isEqualTo(3L);
        assertThat(resp.weeklyRevenue()).hasSize(7);
    }

    @Test
    void buildDashboard_withTodayMetrics_aggregatesCorrectly() {
        DailyJobMetrics m = new DailyJobMetrics(LocalDate.now(), "AC", "110001");
        m.setTotalJobs(5);
        m.setCompletedJobs(4);
        m.setTotalRevenue(new BigDecimal("8000.00"));

        when(metricsRepository.findByMetricDate(LocalDate.now())).thenReturn(List.of(m));
        when(metricsRepository.findByMetricDate(argThat(d -> !d.equals(LocalDate.now()))))
                .thenReturn(List.of());

        DashboardResponse resp = dashboardService.buildDashboard();

        assertThat(resp.todayJobs()).isEqualTo(5);
        assertThat(resp.todayRevenue()).isEqualByComparingTo("8000.00");
        assertThat(resp.doneJobs()).isEqualTo(4);
    }

    @Test
    void getDashboard_cacheHit_returnsWithoutQueryingDb() throws Exception {
        DashboardResponse cached = new DashboardResponse(
                10, new BigDecimal("5000"), 2, 3, 10, 5, 1L,
                List.of(), List.of());
        when(valueOps.get("dashboard:today")).thenReturn(objectMapper.writeValueAsString(cached));

        DashboardResponse resp = dashboardService.getDashboard();

        assertThat(resp.todayJobs()).isEqualTo(10);
        verify(metricsRepository, never()).findByMetricDate(any());
    }

    @Test
    void getJobTrend_buildsTrendForDateRange() {
        LocalDate today = LocalDate.now();
        DailyJobMetrics m = new DailyJobMetrics(today, "WM", "560001");
        m.setTotalJobs(3);
        m.setCompletedJobs(2);
        m.setCancelledJobs(0);
        m.setTotalRevenue(new BigDecimal("3000"));
        when(metricsRepository.findByMetricDateBetweenOrderByMetricDateAsc(any(), any()))
                .thenReturn(List.of(m));

        List<JobTrendEntry> trend = dashboardService.getJobTrend(7);

        assertThat(trend).hasSize(7);
        assertThat(trend.stream().mapToInt(JobTrendEntry::totalJobs).sum()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void getTopParts_returnsTopEntriesFromRepository() {
        UUID partId = UUID.randomUUID();
        Object[] row = {partId.toString(), "AC Filter", 42L};
        doReturn(java.util.Collections.singletonList(row))
                .when(snapshotRepository).findTopPartsBySalesInRange(any(), eq(10));

        var result = dashboardService.getTopParts(30);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).partName()).isEqualTo("AC Filter");
        assertThat(result.get(0).soldCount()).isEqualTo(42);
    }

    @Test
    void storeLiveCounts_setsRedisKeysAndInvalidatesDashboard() {
        when(redis.delete(anyString())).thenReturn(true);

        dashboardService.storeLiveCounts(5, 3, 12);

        verify(valueOps, atLeast(3)).set(anyString(), anyString(), any());
        verify(redis).delete("dashboard:today");
    }
}
