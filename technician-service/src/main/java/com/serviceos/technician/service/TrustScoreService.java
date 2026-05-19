package com.serviceos.technician.service;

import com.serviceos.shared.enums.TechnicianType;
import com.serviceos.technician.entity.Technician;
import com.serviceos.technician.feign.PartsServiceClient;
import com.serviceos.technician.repository.TechnicianRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class TrustScoreService {

    private static final Logger log = LoggerFactory.getLogger(TrustScoreService.class);

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    // Credit limit tiers based on trust score
    private static final BigDecimal TIER_HIGH   = BigDecimal.valueOf(5000);
    private static final BigDecimal TIER_MED    = BigDecimal.valueOf(3000);
    private static final BigDecimal TIER_LOW    = BigDecimal.valueOf(1500);
    private static final BigDecimal TIER_FLOOR  = BigDecimal.valueOf(500);

    private final TechnicianRepository technicianRepository;
    private final PartsServiceClient partsServiceClient;

    public TrustScoreService(TechnicianRepository technicianRepository,
                              PartsServiceClient partsServiceClient) {
        this.technicianRepository = technicianRepository;
        this.partsServiceClient   = partsServiceClient;
    }

    // -------------------------------------------------------------------------
    // Scheduled: runs daily at 02:00 IST for all FREELANCE technicians
    // -------------------------------------------------------------------------

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Kolkata")
    public void recomputeAll() {
        List<Technician> freelancers = technicianRepository
                .findByTypeAndActiveTrue(TechnicianType.FREELANCE);
        log.info("Trust score recompute: {} freelance technicians", freelancers.size());
        freelancers.forEach(t -> recompute(t, true));
    }

    // -------------------------------------------------------------------------
    // On-demand: triggered after credit.updated event
    // -------------------------------------------------------------------------

    @Transactional
    public void recompute(Technician technician, boolean persist) {
        BigDecimal totalPurchased = technician.getTotalPartsPurchased();
        BigDecimal totalPaid      = technician.getTotalPartsPaid();

        // Component 1 – payment reliability: paid / purchased (neutral 0.5 for new technicians)
        BigDecimal paymentReliability = totalPurchased.compareTo(BigDecimal.ZERO) > 0
                ? totalPaid.divide(totalPurchased, 4, RoundingMode.HALF_UP).min(BigDecimal.ONE)
                : BigDecimal.valueOf(0.5);

        // Component 2 – order frequency: normalized on 20 orders as maximum
        BigDecimal orderFrequency = BigDecimal.valueOf(
                Math.min(technician.getPartsOrderCount() / 20.0, 1.0))
                .setScale(4, RoundingMode.HALF_UP);

        // Component 3 – tenure score: months active capped at 24
        long monthsActive = ChronoUnit.MONTHS.between(
                technician.getOnboardedAt().atZone(IST).toLocalDate(), LocalDate.now(IST));
        BigDecimal tenureScore = BigDecimal.valueOf(Math.min(monthsActive / 24.0, 1.0))
                .setScale(4, RoundingMode.HALF_UP);

        // Component 4 – volume score: cumulative purchase amount capped at ₹50,000
        BigDecimal volumeScore = totalPurchased
                .divide(BigDecimal.valueOf(50000), 4, RoundingMode.HALF_UP)
                .min(BigDecimal.ONE);

        // Weighted sum
        BigDecimal score = paymentReliability.multiply(BigDecimal.valueOf(0.40))
                .add(orderFrequency.multiply(BigDecimal.valueOf(0.30)))
                .add(tenureScore.multiply(BigDecimal.valueOf(0.20)))
                .add(volumeScore.multiply(BigDecimal.valueOf(0.10)))
                .setScale(4, RoundingMode.HALF_UP);

        technician.setPaymentReliability(paymentReliability);
        technician.setOrderFrequency(orderFrequency);
        technician.setTenureScore(tenureScore);
        technician.setVolumeScore(volumeScore);
        technician.setTrustScore(score);
        technician.setLastTrustComputed(Instant.now());

        // Update credit limit based on trust tier — only if admin has already enabled credit (limit > 0).
        // New technicians start at 0 and are not given credit automatically.
        if (technician.getCreditLimit().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal newLimit = creditLimitForScore(score.doubleValue());
            technician.setCreditLimit(newLimit);
            if (persist) {
                technicianRepository.save(technician);
            }
            syncCreditLimitToPartsService(technician.getId(), newLimit);
        } else {
            if (persist) {
                technicianRepository.save(technician);
            }
        }
    }

    // -------------------------------------------------------------------------

    private BigDecimal creditLimitForScore(double score) {
        if (score >= 0.80) return TIER_HIGH;
        if (score >= 0.60) return TIER_MED;
        if (score >= 0.40) return TIER_LOW;
        return TIER_FLOOR;
    }

    private void syncCreditLimitToPartsService(java.util.UUID technicianId, BigDecimal limit) {
        try {
            partsServiceClient.updateCreditLimit(technicianId, limit);
            log.debug("Synced credit limit {} to parts-service for technician {}", limit, technicianId);
        } catch (Exception ex) {
            log.warn("Failed to sync credit limit to parts-service for technician {}: {}",
                    technicianId, ex.getMessage());
        }
    }
}
