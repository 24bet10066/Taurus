package com.serviceos.payment.dto.response;

import com.serviceos.payment.entity.PaymentRecord;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CashReconciliationResponse(
        UUID technicianId,
        LocalDate date,
        BigDecimal totalCashCollected,
        List<PaymentRecord> records
) {}
