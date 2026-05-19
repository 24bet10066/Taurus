package com.serviceos.payment.service;

import com.serviceos.payment.entity.PaymentRecord;
import com.serviceos.shared.enums.PaymentMethod;
import com.serviceos.shared.event.PaymentReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPaymentReceived(PaymentRecord payment) {
        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(payment.getPaymentMethod());
        } catch (IllegalArgumentException e) {
            method = PaymentMethod.CASH;
        }
        var event = new PaymentReceivedEvent(
                payment.getId(),
                payment.getJobId(),
                null, // customerId not stored on payment; phone is what notification-service needs
                payment.getCustomerPhone(),
                payment.getAmount(),
                method,
                payment.getRazorpayPaymentId(),
                payment.getCompletedAt() != null ? payment.getCompletedAt() : Instant.now()
        );
        kafkaTemplate.send(PaymentReceivedEvent.TOPIC, payment.getJobId().toString(), event)
                .whenComplete((r, ex) -> {
                    if (ex != null) log.error("Failed to publish payment.received: {}", ex.getMessage());
                    else log.debug("Published payment.received for paymentId={}", payment.getId());
                });
    }
}
