package com.serviceos.notification.consumer;

import com.serviceos.notification.channel.NotificationDispatcher;
import com.serviceos.shared.enums.JobStatus;
import com.serviceos.shared.event.JobStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class JobStatusChangedConsumer {

    private static final Logger log = LoggerFactory.getLogger(JobStatusChangedConsumer.class);

    private final NotificationDispatcher dispatcher;
    private final String shopPhone;

    public JobStatusChangedConsumer(NotificationDispatcher dispatcher,
                                    @Value("${serviceos.notification.shop-phone:}") String shopPhone) {
        this.dispatcher = dispatcher;
        this.shopPhone  = shopPhone;
    }

    @KafkaListener(topics = JobStatusChangedEvent.TOPIC, groupId = "notification-service")
    public void onStatusChanged(JobStatusChangedEvent event) {
        log.debug("Consumed job.status.changed jobId={} to={}", event.jobId(), event.to());
        String customerPhone = event.customerPhone();
        JobStatus to = event.to();
        boolean emergency = event.isEmergency();

        switch (to) {
            case IN_TRANSIT -> {
                if (customerPhone == null) return;
                String techName  = nvl(event.techName(), "Technician");
                String techPhone = nvl(event.techPhone(), "N/A");
                String msg = "Aapka technician " + techName + " aa rahe hain. Phone: " + techPhone;
                var r = dispatcher.dispatch(customerPhone, msg, "JOB_ASSIGNED", emergency);
                log.info("IN_TRANSIT notification: jobId={} sent={} ch={}", event.jobId(), r.success(), r.channel());
            }
            case AT_CUSTOMER -> {
                if (customerPhone == null) return;
                var r = dispatcher.dispatch(customerPhone,
                        "Technician aapke paas pahunch gaya. Problem dekh rahe hain.",
                        "JOB_ASSIGNED", emergency);
                log.info("AT_CUSTOMER notification: jobId={} sent={}", event.jobId(), r.success());
            }
            case PARTS_NEEDED -> {
                if (customerPhone == null) return;
                var r = dispatcher.dispatch(customerPhone,
                        "Part ki zaroorat hai. Thoda time lagega. Hum update karenge.",
                        "JOB_ASSIGNED", emergency);
                log.info("PARTS_NEEDED notification: jobId={} sent={}", event.jobId(), r.success());
            }
            case COMPLETED -> {
                if (customerPhone == null) return;
                String amount = event.totalCharge() != null ? event.totalCharge().toPlainString() : "?";
                var r = dispatcher.dispatch(customerPhone,
                        "Kaam ho gaya! Charge: ₹" + amount + ". Shukriya!",
                        "JOB_COMPLETED", emergency);
                log.info("COMPLETED notification: jobId={} sent={}", event.jobId(), r.success());
            }
            case CANCELLED -> {
                if (customerPhone == null) return;
                String msg = "Aapki booking cancel ho gayi. Dobara book karein ya call karein: " + shopPhone;
                dispatcher.dispatchBoth(customerPhone, msg, "JOB_CANCELLED", emergency);
                log.info("CANCELLED notification: jobId={} both channels attempted", event.jobId());
            }
            default -> { /* no notification for other transitions */ }
        }
    }

    private static String nvl(String val, String fallback) {
        return val != null && !val.isBlank() ? val : fallback;
    }
}
