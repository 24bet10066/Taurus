package com.serviceos.job.service;

import com.serviceos.job.entity.Job;
import com.serviceos.job.repository.JobPartUsedRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Computes the final charge for a job from its component parts.
 * All base values are read dynamically from the business config service — no hardcoded amounts.
 *
 * final_charge = base_charge + travel_surcharge + urgency_surcharge + parts_charge + labor_charge - discount
 */
@Service
public class ChargeCalculationService {

    private static final Logger log = LoggerFactory.getLogger(ChargeCalculationService.class);

    private final ConfigClient configClient;
    private final JobPartUsedRepository jobPartUsedRepository;

    public ChargeCalculationService(ConfigClient configClient,
                                    JobPartUsedRepository jobPartUsedRepository) {
        this.configClient = configClient;
        this.jobPartUsedRepository = jobPartUsedRepository;
    }

    /**
     * Sets base_charge and travel_surcharge from config, then recomputes final_charge.
     * Call on job creation; also call whenever labor_charge, urgency_surcharge, or discount change.
     */
    public void applyCharges(Job job) {
        applyBaseCharge(job);
        applyTravelSurcharge(job);
        computeFinalCharge(job);
    }

    /** Apply minimum service charge from config. */
    public void applyBaseCharge(Job job) {
        int min = configClient.getInt("booking.service_charge_minimum", 300);
        job.setBaseCharge(BigDecimal.valueOf(min));
    }

    /**
     * Area-based travel surcharge: compare job area against shop city from config.
     * Exact match → city (no extra). Non-blank area that differs → town surcharge.
     * Null/blank area → assume city.
     */
    public void applyTravelSurcharge(Job job) {
        String shopCity = configClient.get("shop.city", "Banda").trim().toLowerCase();
        String jobArea  = job.getArea();

        if (jobArea == null || jobArea.isBlank()) {
            job.setTravelSurcharge(BigDecimal.ZERO);
            return;
        }
        String area = jobArea.trim().toLowerCase();
        if (area.contains(shopCity)) {
            job.setTravelSurcharge(BigDecimal.ZERO);
        } else {
            int townExtra = configClient.getInt("booking.travel_extra_town", 50);
            job.setTravelSurcharge(BigDecimal.valueOf(townExtra));
            log.debug("Travel surcharge applied: jobId={} area={} surcharge={}",
                    job.getId(), job.getArea(), townExtra);
        }
    }

    /**
     * Recompute final_charge from all components. Safe to call multiple times.
     */
    public void computeFinalCharge(Job job) {
        BigDecimal base     = safe(job.getBaseCharge());
        BigDecimal travel   = safe(job.getTravelSurcharge());
        BigDecimal urgency  = safe(job.getUrgencySurcharge());
        BigDecimal parts    = safe(job.getPartsCharge());
        BigDecimal labor    = safe(job.getLaborCharge());
        BigDecimal discount = safe(job.getDiscount());

        BigDecimal total = base.add(travel).add(urgency).add(parts).add(labor).subtract(discount);
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

        job.setFinalCharge(total);
        job.setActualCharge(total); // keep actual_charge in sync for payment service compatibility
        log.debug("Charges computed: jobId={} base={} travel={} urgency={} parts={} labor={} discount={} final={}",
                job.getId(), base, travel, urgency, parts, labor, discount, total);
    }

    private static BigDecimal safe(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
