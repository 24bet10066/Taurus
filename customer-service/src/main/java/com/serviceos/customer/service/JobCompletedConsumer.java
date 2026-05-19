package com.serviceos.customer.service;

import com.serviceos.shared.event.JobCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class JobCompletedConsumer {

    private static final Logger log = LoggerFactory.getLogger(JobCompletedConsumer.class);

    private final CustomerService customerService;

    public JobCompletedConsumer(CustomerService customerService) {
        this.customerService = customerService;
    }

    @KafkaListener(topics = JobCompletedEvent.TOPIC, groupId = "customer-service")
    public void onJobCompleted(JobCompletedEvent event) {
        if (event.customerId() == null) return;
        log.debug("job.completed received: jobId={} customerId={} total={}",
                event.jobId(), event.customerId(), event.totalAmount());
        customerService.recordJobCompletion(
                event.customerId(), event.totalAmount(), event.completedAt());
    }
}
