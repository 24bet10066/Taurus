package com.serviceos.payment.service;

import com.serviceos.payment.dto.response.DailySummaryResponse;
import com.serviceos.payment.entity.DailySummary;
import com.serviceos.payment.entity.PaymentRecord;
import com.serviceos.payment.repository.DailySummaryRepository;
import com.serviceos.payment.repository.PaymentRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class DailySummaryService {

    private static final Logger log = LoggerFactory.getLogger(DailySummaryService.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final PaymentRecordRepository paymentRepository;
    private final DailySummaryRepository summaryRepository;

    public DailySummaryService(PaymentRecordRepository paymentRepository,
                                DailySummaryRepository summaryRepository) {
        this.paymentRepository = paymentRepository;
        this.summaryRepository = summaryRepository;
    }

    @Scheduled(cron = "0 55 23 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void aggregateToday() {
        LocalDate today = LocalDate.now(IST);
        log.info("Running daily summary aggregation for {}", today);
        aggregate(today);
    }

    @Transactional
    public DailySummaryResponse getSummary(LocalDate date) {
        DailySummary summary = summaryRepository.findBySummaryDate(date)
                .orElseGet(() -> {
                    // Aggregate on-demand if not yet computed
                    aggregate(date);
                    return summaryRepository.findBySummaryDate(date).orElseGet(DailySummary::new);
                });
        return toResponse(summary, date);
    }

    private void aggregate(LocalDate date) {
        Instant start = date.atStartOfDay(IST).toInstant();
        Instant end   = date.plusDays(1).atStartOfDay(IST).toInstant();

        List<PaymentRecord> records = paymentRepository.findCompletedBetween(start, end);

        BigDecimal totalRevenue  = BigDecimal.ZERO;
        BigDecimal cashRevenue   = BigDecimal.ZERO;
        BigDecimal onlineRevenue = BigDecimal.ZERO;

        for (PaymentRecord r : records) {
            totalRevenue = totalRevenue.add(r.getAmount());
            if ("CASH".equalsIgnoreCase(r.getPaymentMethod())) {
                cashRevenue = cashRevenue.add(r.getAmount());
            } else {
                onlineRevenue = onlineRevenue.add(r.getAmount());
            }
        }

        DailySummary summary = summaryRepository.findBySummaryDate(date)
                .orElseGet(DailySummary::new);
        summary.setSummaryDate(date);
        summary.setTotalRevenue(totalRevenue);
        summary.setTotalJobs(records.size());
        summary.setCashRevenue(cashRevenue);
        summary.setOnlineRevenue(onlineRevenue);
        summary.setPartsRevenue(BigDecimal.ZERO);  // enriched from job-service if needed
        summary.setLaborRevenue(BigDecimal.ZERO);
        summaryRepository.save(summary);
        log.info("Daily summary for {}: total={} jobs={}", date, totalRevenue, records.size());
    }

    private DailySummaryResponse toResponse(DailySummary s, LocalDate date) {
        return new DailySummaryResponse(
                s.getSummaryDate() != null ? s.getSummaryDate() : date,
                nvl(s.getTotalRevenue()), s.getTotalJobs(),
                nvl(s.getCashRevenue()), nvl(s.getOnlineRevenue()),
                nvl(s.getPartsRevenue()), nvl(s.getLaborRevenue())
        );
    }

    private static BigDecimal nvl(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}
