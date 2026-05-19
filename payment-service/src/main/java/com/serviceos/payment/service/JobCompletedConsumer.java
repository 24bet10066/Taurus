package com.serviceos.payment.service;

import com.serviceos.shared.event.JobCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class JobCompletedConsumer {

    private static final Logger log = LoggerFactory.getLogger(JobCompletedConsumer.class);

    private final PaymentService paymentService;

    public JobCompletedConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = JobCompletedEvent.TOPIC, groupId = "payment-service")
    public void onJobCompleted(JobCompletedEvent event) {
        log.debug("Consumed job.completed jobId={} total={}", event.jobId(), event.totalAmount());
        paymentService.createPendingRecord(event.jobId(), event.totalAmount(), null);
    }
}
