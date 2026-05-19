package com.serviceos.technician.service;

import com.serviceos.shared.enums.TechnicianType;
import com.serviceos.technician.entity.Technician;
import com.serviceos.technician.feign.PartsServiceClient;
import com.serviceos.technician.repository.TechnicianRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TrustScoreServiceTest {

    @Mock TechnicianRepository technicianRepository;
    @Mock PartsServiceClient partsServiceClient;

    TrustScoreService trustScoreService;

    private Technician technician;

    @BeforeEach
    void setUp() {
        trustScoreService = new TrustScoreService(technicianRepository, partsServiceClient);

        technician = new Technician();
        technician.setId(UUID.randomUUID());
        technician.setType(TechnicianType.FREELANCE);
        technician.setTotalPartsPurchased(BigDecimal.ZERO);
        technician.setTotalPartsPaid(BigDecimal.ZERO);
        technician.setPartsOrderCount(0);
        technician.setTrustScore(BigDecimal.valueOf(0.5));
        technician.setOnboardedAt(Instant.now().minus(365, ChronoUnit.DAYS)); // 1 year tenure
        when(technicianRepository.save(any())).thenReturn(technician);
        doNothing().when(partsServiceClient).updateCreditLimit(any(), any());
    }

    @Test
    void newTechnician_getsNeutralPaymentReliability() {
        trustScoreService.recompute(technician, false);

        // paymentReliability = 0.5 (neutral for 0 purchases)
        assertThat(technician.getPaymentReliability()).isEqualByComparingTo("0.5000");
    }

    @Test
    void perfectPaymentHistory_maxReliability() {
        technician.setTotalPartsPurchased(new BigDecimal("10000"));
        technician.setTotalPartsPaid(new BigDecimal("10000"));
        trustScoreService.recompute(technician, false);

        assertThat(technician.getPaymentReliability()).isEqualByComparingTo("1.0000");
    }

    @Test
    void partialPayment_correctReliabilityRatio() {
        technician.setTotalPartsPurchased(new BigDecimal("1000"));
        technician.setTotalPartsPaid(new BigDecimal("600"));
        trustScoreService.recompute(technician, false);

        assertThat(technician.getPaymentReliability()).isEqualByComparingTo("0.6000");
    }

    @Test
    void orderFrequency_cappedAt20Orders() {
        technician.setPartsOrderCount(20);
        trustScoreService.recompute(technician, false);
        assertThat(technician.getOrderFrequency()).isEqualByComparingTo("1.0000");

        technician.setPartsOrderCount(100);
        trustScoreService.recompute(technician, false);
        assertThat(technician.getOrderFrequency()).isEqualByComparingTo("1.0000");
    }

    @Test
    void orderFrequency_proportional() {
        technician.setPartsOrderCount(10);
        trustScoreService.recompute(technician, false);
        assertThat(technician.getOrderFrequency()).isEqualByComparingTo("0.5000");
    }

    @Test
    void tenureScore_proportionalToMonths() {
        // 1 year = 12 months → 12/24 = 0.5
        trustScoreService.recompute(technician, false);
        assertThat(technician.getTenureScore()).isBetween(
                BigDecimal.valueOf(0.45), BigDecimal.valueOf(0.55));
    }

    @Test
    void volumeScore_cappedAt50000() {
        technician.setTotalPartsPurchased(new BigDecimal("50000"));
        trustScoreService.recompute(technician, false);
        assertThat(technician.getVolumeScore()).isEqualByComparingTo("1.0000");

        technician.setTotalPartsPurchased(new BigDecimal("100000"));
        trustScoreService.recompute(technician, false);
        assertThat(technician.getVolumeScore()).isEqualByComparingTo("1.0000");
    }

    @Test
    void trustScore_computedFromWeightedComponents() {
        // Set up known values: reliability=1.0, freq=1.0, tenure≈0.5, volume=0.2
        technician.setTotalPartsPurchased(new BigDecimal("10000"));
        technician.setTotalPartsPaid(new BigDecimal("10000"));
        technician.setPartsOrderCount(20);
        // 1-year tenure → ≈0.5 tenureScore

        trustScoreService.recompute(technician, false);

        double score = technician.getTrustScore().doubleValue();
        // Expected ≈ 1.0*0.40 + 1.0*0.30 + ~0.5*0.20 + 0.2*0.10 = 0.40+0.30+0.10+0.02 = 0.82
        assertThat(score).isBetween(0.70, 0.95);
    }

    @Test
    void highTrustScore_assignsHighCreditTier() {
        technician.setTotalPartsPurchased(new BigDecimal("50000"));
        technician.setTotalPartsPaid(new BigDecimal("50000"));
        technician.setPartsOrderCount(20);
        technician.setOnboardedAt(Instant.now().minus(730, ChronoUnit.DAYS)); // 2 years

        trustScoreService.recompute(technician, false);

        assertThat(technician.getTrustScore().doubleValue()).isGreaterThanOrEqualTo(0.80);
        assertThat(technician.getCreditLimit()).isEqualByComparingTo("5000");
    }

    @Test
    void lowTrustScore_assignsFloorCreditTier() {
        // All zeros → score ~0.1 from tenure only
        technician.setOnboardedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        trustScoreService.recompute(technician, false);

        assertThat(technician.getTrustScore().doubleValue()).isLessThan(0.40);
        assertThat(technician.getCreditLimit()).isEqualByComparingTo("500");
    }

    @Test
    void recompute_withPersist_savesToRepository() {
        trustScoreService.recompute(technician, true);
        verify(technicianRepository).save(technician);
    }

    @Test
    void recompute_withoutPersist_doesNotSave() {
        trustScoreService.recompute(technician, false);
        verify(technicianRepository, never()).save(any());
    }
}
