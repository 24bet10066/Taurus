package com.serviceos.shared.exception;

import java.math.BigDecimal;
import java.util.UUID;

public class CreditLimitExceededException extends RuntimeException {

    private final UUID technicianId;
    private final BigDecimal requested;
    private final BigDecimal available;
    private final BigDecimal creditLimit;

    public CreditLimitExceededException(UUID technicianId,
                                        BigDecimal requested,
                                        BigDecimal available,
                                        BigDecimal creditLimit) {
        super("Credit limit exceeded for technician %s: requested %s, available %s, limit %s"
                .formatted(technicianId, requested, available, creditLimit));
        this.technicianId = technicianId;
        this.requested = requested;
        this.available = available;
        this.creditLimit = creditLimit;
    }

    public UUID getTechnicianId() {
        return technicianId;
    }

    public BigDecimal getRequested() {
        return requested;
    }

    public BigDecimal getAvailable() {
        return available;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }
}
