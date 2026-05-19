package com.serviceos.notification.consumer;

import com.serviceos.notification.channel.NotificationDispatcher;
import com.serviceos.shared.event.InventoryReorderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryReorderConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryReorderConsumer.class);

    private final NotificationDispatcher dispatcher;
    private final String adminWhatsapp;

    public InventoryReorderConsumer(NotificationDispatcher dispatcher,
                                    @Value("${serviceos.notification.admin-whatsapp:}") String adminWhatsapp) {
        this.dispatcher    = dispatcher;
        this.adminWhatsapp = adminWhatsapp;
    }

    @KafkaListener(topics = InventoryReorderEvent.TOPIC, groupId = "notification-service")
    public void onInventoryReorder(InventoryReorderEvent event) {
        log.debug("Consumed inventory.reorder-alert partId={}", event.partId());
        if (adminWhatsapp == null || adminWhatsapp.isBlank()) {
            log.warn("ADMIN_WHATSAPP not configured — skipping stock alert for part={}", event.partName());
            return;
        }
        String msg = "Stock alert: " + event.partName() + " sirf " + event.currentStock() +
                " left. Minimum: " + event.reorderLevel() + ". Order karo.";
        // Admin alerts bypass SMS cooldown (dispatchAdmin skips cooldown entirely)
        var result = dispatcher.dispatchAdmin(adminWhatsapp, msg);
        log.info("inventory.reorder notification: partId={} sent={} ch={}",
                event.partId(), result.success(), result.channel());
    }
}
