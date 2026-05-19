package com.serviceos.payment.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderResponse(
        String razorpayOrderId,
        BigDecimal amount,
        String currency,
        UUID jobId,
        String keyId
) {}
