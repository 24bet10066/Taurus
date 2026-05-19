package com.serviceos.notification.consumer;

import com.serviceos.notification.channel.NotificationDispatcher;
import com.serviceos.shared.event.JobCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class JobCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(JobCreatedConsumer.class);

    private final NotificationDispatcher dispatcher;

    public JobCreatedConsumer(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @KafkaListener(topics = JobCreatedEvent.TOPIC, groupId = "notification-service")
    public void onJobCreated(JobCreatedEvent event) {
        log.debug("Consumed job.created jobId={}", event.jobId());
        if (event.customerPhone() == null) {
            log.warn("job.created missing customerPhone — skipping for jobId={}", event.jobId());
            return;
        }
        String appliance = event.applianceType() != null ? event.applianceType().name() : "appliance";
        String name = event.customerName() != null ? event.customerName() : "Customer";
        String msg = "Namaste " + name + " ji! Aapki " + appliance +
                " repair request mil gayi. Hum jald hi technician bhejenge. Booking ID: " + event.jobId();

        var result = dispatcher.dispatch(event.customerPhone(), msg, "JOB_CREATED", event.isEmergency());
        log.info("job.created notification: jobId={} sent={} ch={} suppressed={}",
                event.jobId(), result.success(), result.channel(), result.suppressed());
    }
}
