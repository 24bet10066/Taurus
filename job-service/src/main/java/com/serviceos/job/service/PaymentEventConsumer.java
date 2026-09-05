package com.serviceos.job.service;

import com.serviceos.job.dto.request.PaymentRequest;
import com.serviceos.shared.event.PaymentReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final JobService jobService;

    public PaymentEventConsumer(JobService jobService) {
        this.jobService = jobService;
    }

    @KafkaListener(topics = PaymentReceivedEvent.TOPIC, groupId = "job-service")
    public void handlePaymentReceived(PaymentReceivedEvent event) {
        log.info("Received payment.received event for jobId: {}", event.jobId());
        try {
            PaymentRequest req = new PaymentRequest(
                    event.method().name(),
                    event.gatewayRef(),
                    event.amount()
            );
            jobService.confirmPayment(event.jobId(), req);
            log.info("Successfully updated payment status for jobId: {}", event.jobId());
        } catch (Exception e) {
            log.error("Failed to process payment.received event for jobId: {}", event.jobId(), e);
        }
    }
}
