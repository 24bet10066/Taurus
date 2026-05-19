package com.serviceos.parts.service;

import com.serviceos.parts.dto.request.CreditPaymentRequest;
import com.serviceos.parts.dto.request.UpdateCreditLimitRequest;
import com.serviceos.parts.dto.response.CreditTransactionResponse;
import com.serviceos.parts.dto.response.TechnicianCreditPage;
import com.serviceos.parts.entity.CreditTransaction;
import com.serviceos.parts.entity.TechnicianCredit;
import com.serviceos.parts.repository.CreditTransactionRepository;
import com.serviceos.parts.repository.TechnicianCreditRepository;
import com.serviceos.shared.exception.BusinessRuleViolationException;
import com.serviceos.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class CreditService {

    private static final Logger log = LoggerFactory.getLogger(CreditService.class);
    private static final int OVERDUE_DAYS = 30;

    private final TechnicianCreditRepository creditRepository;
    private final CreditTransactionRepository txRepository;
    private final PartsEventPublisher eventPublisher;

    public CreditService(TechnicianCreditRepository creditRepository,
                         CreditTransactionRepository txRepository,
                         PartsEventPublisher eventPublisher) {
        this.creditRepository = creditRepository;
        this.txRepository = txRepository;
        this.eventPublisher = eventPublisher;
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TechnicianCreditPage getCreditPage(UUID technicianId) {
        TechnicianCredit credit = requireCredit(technicianId);
        List<CreditTransaction> recent = txRepository.findByTechnicianIdOrderByCreatedAtDesc(
                technicianId, PageRequest.of(0, 20));
        List<CreditTransactionResponse> txDtos = recent.stream().map(this::toTxDto).toList();
        return new TechnicianCreditPage(
                credit.getTechnicianId(), credit.getTechnicianName(), credit.getTechnicianPhone(),
                credit.getCurrentBalance(), credit.getCreditLimit(),
                credit.getCreditLimit().subtract(credit.getCurrentBalance()),
                credit.getTotalPurchased(), credit.getTotalPaid(),
                credit.getLastPurchaseAt(), credit.getLastPaymentAt(), txDtos
        );
    }

    // ── Debit (called from SalesService within the same transaction) ──────────

    @Transactional(propagation = Propagation.MANDATORY)
    public TechnicianCredit debitCredit(UUID technicianId, BigDecimal amount,
                                        UUID saleId, String recordedBy) {
        TechnicianCredit credit = requireCredit(technicianId);

        // Guard: credit not enabled
        if (credit.getCreditLimit().compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessRuleViolationException(
                    "CREDIT_NOT_ENABLED", "Credit not enabled. Contact SK Electronics.");
        }

        // Guard: overdue blocks new purchases
        checkAndMarkOverdue(credit);
        if (credit.isOverdue()) {
            throw new BusinessRuleViolationException(
                    "CREDIT_OVERDUE", "Credit account overdue. Pay outstanding balance of ₹"
                    + credit.getCurrentBalance().toPlainString() + " to continue.");
        }

        // Guard: credit limit reached
        if (credit.getCurrentBalance().compareTo(credit.getCreditLimit()) >= 0) {
            throw new BusinessRuleViolationException(
                    "CREDIT_LIMIT_REACHED", "Credit limit reached. Pay ₹"
                    + credit.getCurrentBalance().toPlainString() + " to continue.");
        }

        BigDecimal newBalance = credit.getCurrentBalance().add(amount);
        credit.setCurrentBalance(newBalance);
        credit.setTotalPurchased(credit.getTotalPurchased().add(amount));
        credit.setLastPurchaseAt(Instant.now());
        TechnicianCredit saved = creditRepository.save(credit);

        appendTransaction(technicianId, "DEBIT", amount, newBalance, null, saleId, null, recordedBy);
        eventPublisher.publishCreditUpdated(technicianId, amount, newBalance,
                credit.getCreditLimit(), "PURCHASE_ON_CREDIT", saleId);
        return saved;
    }

    // ── Payment ───────────────────────────────────────────────────────────────

    @Transactional
    public TechnicianCreditPage recordPayment(UUID technicianId, CreditPaymentRequest req,
                                              String recordedBy) {
        TechnicianCredit credit = requireCredit(technicianId);
        BigDecimal newBalance = credit.getCurrentBalance().subtract(req.amount());
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) newBalance = BigDecimal.ZERO;

        credit.setCurrentBalance(newBalance);
        credit.setTotalPaid(credit.getTotalPaid().add(req.amount()));
        credit.setLastPaymentAt(Instant.now());

        // Clear overdue flag if balance is now zero
        if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
            credit.setOverdue(false);
            credit.setOverdueSince(null);
        }

        creditRepository.save(credit);
        appendTransaction(technicianId, "PAYMENT", req.amount(), newBalance,
                req.paymentMethod(), null, req.notes(), recordedBy);

        eventPublisher.publishCreditUpdated(technicianId, req.amount().negate(), newBalance,
                credit.getCreditLimit(), "PAYMENT_RECEIVED", null);

        return getCreditPage(technicianId);
    }

    // ── Admin: set credit limit ───────────────────────────────────────────────

    @Transactional
    public TechnicianCreditPage setCreditLimit(UUID technicianId, UpdateCreditLimitRequest req,
                                               String adminId) {
        TechnicianCredit credit = requireCredit(technicianId);
        BigDecimal oldLimit = credit.getCreditLimit();
        credit.setCreditLimit(req.newLimit());
        creditRepository.save(credit);

        String notes = "Limit changed from ₹" + oldLimit + " to ₹" + req.newLimit()
                + ". Reason: " + req.reason();
        appendTransaction(technicianId, "LIMIT_CHANGE", req.newLimit(), credit.getCurrentBalance(),
                null, null, notes, adminId);

        log.info("Credit limit set: techId={} old={} new={} by={} reason={}",
                technicianId, oldLimit, req.newLimit(), adminId, req.reason());
        return getCreditPage(technicianId);
    }

    // ── Overdue detection ─────────────────────────────────────────────────────

    private void checkAndMarkOverdue(TechnicianCredit credit) {
        if (credit.isOverdue()) return;
        if (credit.getCurrentBalance().compareTo(BigDecimal.ZERO) <= 0) return;
        if (credit.getLastPurchaseAt() == null) return;

        long daysSince = ChronoUnit.DAYS.between(credit.getLastPurchaseAt(), Instant.now());
        if (daysSince > OVERDUE_DAYS) {
            credit.setOverdue(true);
            credit.setOverdueSince(Instant.now());
            creditRepository.save(credit);
            log.warn("Technician {} marked OVERDUE: balance={} daysSinceLastPurchase={}",
                    credit.getTechnicianId(), credit.getCurrentBalance(), daysSince);
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void appendTransaction(UUID techId, String type, BigDecimal amount,
                                   BigDecimal balanceAfter, String paymentMethod,
                                   UUID referenceId, String notes, String recordedBy) {
        CreditTransaction tx = new CreditTransaction();
        tx.setTechnicianId(techId);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setBalanceAfter(balanceAfter);
        tx.setPaymentMethod(paymentMethod);
        tx.setReferenceId(referenceId);
        tx.setNotes(notes);
        tx.setRecordedBy(recordedBy);
        txRepository.save(tx);
    }

    private TechnicianCredit requireCredit(UUID technicianId) {
        return creditRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("TechnicianCredit", technicianId));
    }

    private CreditTransactionResponse toTxDto(CreditTransaction tx) {
        return new CreditTransactionResponse(tx.getId(), tx.getType(), tx.getAmount(),
                tx.getBalanceAfter(), tx.getPaymentMethod(), tx.getReferenceId(),
                tx.getNotes(), tx.getCreatedAt());
    }
}
