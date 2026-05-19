package com.serviceos.notification.consumer;

import com.serviceos.notification.channel.NotificationDispatcher;
import com.serviceos.notification.event.AmcOpportunityPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AmcOpportunityConsumer {

    private static final Logger log = LoggerFactory.getLogger(AmcOpportunityConsumer.class);

    private final NotificationDispatcher dispatcher;

    public AmcOpportunityConsumer(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @KafkaListener(topics = "customer.amc-opportunity", groupId = "notification-service")
    public void onAmcOpportunity(AmcOpportunityPayload event) {
        log.debug("Consumed customer.amc-opportunity customerId={}", event.customerId());
        if (event.customerPhone() == null) {
            log.warn("amc-opportunity missing customerPhone for customerId={}", event.customerId());
            return;
        }
        String name     = event.customerName() != null ? event.customerName() : "Customer";
        String appliance = event.applianceType() != null ? event.applianceType().name() : "appliance";
        String msg = "Namaste " + name + " ji! Aapki " + appliance +
                " ki service 6 mahine mein nahi hui. AMC ke liye call karein.";
        var result = dispatcher.dispatch(event.customerPhone(), msg, "AMC_OPPORTUNITY", false);
        log.info("amc-opportunity notification: customerId={} sent={} ch={}",
                event.customerId(), result.success(), result.channel());
    }
}
