package com.serviceos.notification.consumer;

import com.serviceos.notification.channel.NotificationDispatcher;
import com.serviceos.shared.enums.TechnicianType;
import com.serviceos.shared.event.JobAssignedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class JobAssignedConsumer {

    private static final Logger log = LoggerFactory.getLogger(JobAssignedConsumer.class);

    private final NotificationDispatcher dispatcher;

    public JobAssignedConsumer(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @KafkaListener(topics = JobAssignedEvent.TOPIC, groupId = "notification-service")
    public void onJobAssigned(JobAssignedEvent event) {
        log.debug("Consumed job.assigned jobId={} techId={}", event.jobId(), event.technicianId());

        if (event.technicianType() != TechnicianType.HIRED) return;
        if (event.technicianPhone() == null) {
            log.warn("job.assigned missing technicianPhone — skipping for jobId={}", event.jobId());
            return;
        }

        String appliance = event.applianceType() != null ? event.applianceType().name() : "appliance";
        String area      = nvl(event.area(), "location");
        String name      = nvl(event.customerName(), "Customer");
        String phone     = nvl(event.customerPhone(), "N/A");
        String issue     = nvl(event.problemDescription(), "repair");

        String msg = "Naya kaam: " + name + ", " + area +
                ". Phone: " + phone +
                ". Appliance: " + appliance +
                ". Problem: " + issue + ". Jaldi jaiye!";

        var result = dispatcher.dispatch(event.technicianPhone(), msg, "JOB_ASSIGNED", event.isEmergency());
        log.info("job.assigned tech-notification: jobId={} sent={} ch={} suppressed={}",
                event.jobId(), result.success(), result.channel(), result.suppressed());
    }

    private static String nvl(String val, String fallback) {
        return val != null && !val.isBlank() ? val : fallback;
    }
}
