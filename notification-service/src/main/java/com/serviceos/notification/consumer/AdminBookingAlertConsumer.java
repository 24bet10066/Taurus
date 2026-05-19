package com.serviceos.notification.consumer;

import com.serviceos.notification.channel.SmsChannel;
import com.serviceos.notification.channel.WhatsAppChannel;
import com.serviceos.shared.event.JobCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Sends a push alert (SMS + best-effort WhatsApp) to the shop owner(s) the
 * instant a customer books online.
 *
 * Why SMS first: father is 50, Banda, may not have WhatsApp open. SMS is
 * universal, native push-notification, no opt-in, no Meta approval, ₹0.30
 * per message. WhatsApp is attempted in parallel as a bonus channel.
 *
 * Configured admin phones in application.yml:
 *   serviceos.notification.admin-alert-phones: "8960245022,6306557517"
 *
 * If father is busy on a call, Raj sees the same SMS within 5 seconds and
 * can tell him verbally — no need for father to open the app constantly.
 *
 * Cooldown is intentionally bypassed (emergency=true): each booking is a
 * distinct event that the shop must not miss.
 */
@Component
public class AdminBookingAlertConsumer {

    private static final Logger log = LoggerFactory.getLogger(AdminBookingAlertConsumer.class);

    private final SmsChannel smsChannel;
    private final WhatsAppChannel waChannel;
    private final List<String> adminPhones;

    public AdminBookingAlertConsumer(
            SmsChannel smsChannel,
            WhatsAppChannel waChannel,
            @Value("${serviceos.notification.admin-alert-phones:}") String adminPhonesCsv) {
        this.smsChannel = smsChannel;
        this.waChannel = waChannel;
        this.adminPhones = Arrays.stream(adminPhonesCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
        if (this.adminPhones.isEmpty()) {
            log.warn("admin-alert-phones is empty — admin booking alerts will NOT be sent. "
                   + "Set serviceos.notification.admin-alert-phones in application.yml.");
        } else {
            log.info("Admin booking alerts will fire to {} phone(s)", this.adminPhones.size());
        }
    }

    @KafkaListener(topics = JobCreatedEvent.TOPIC, groupId = "notification-service-admin-alert")
    public void onJobCreated(JobCreatedEvent event) {
        if (adminPhones.isEmpty()) return;

        String shortId = shortId(event.jobId());
        String appliance = event.applianceType() != null ? event.applianceType().name() : "appliance";
        String name = event.customerName() != null && !event.customerName().isBlank()
                ? event.customerName()
                : "Customer";
        String phone = event.customerPhone() != null ? event.customerPhone() : "—";
        String area = event.address() != null && !event.address().isBlank()
                ? event.address()
                : "no address";

        // Branded SMS — recipients see "SK Electronics" so they recognise it instantly.
        // Kept under 160 chars where possible. Hindi-friendly Latin chars only.
        String sms = "SK Electronics: NEW booking #" + shortId
                + " - " + appliance + " repair"
                + " - " + name + " " + phone
                + " - " + truncate(area, 40)
                + ". Open admin to assign.";

        // Fan out to every admin number.
        for (String adminPhone : adminPhones) {
            boolean smsSent = smsChannel.send(adminPhone, sms);
            boolean waSent  = waChannel.send(adminPhone, sms);
            log.info("Admin booking alert: jobId={} to={} sms={} wa={}",
                    event.jobId(), maskPhone(adminPhone), smsSent, waSent);
        }
    }

    private static String shortId(UUID id) {
        if (id == null) return "?";
        String s = id.toString().replace("-", "");
        return s.substring(s.length() - 6).toUpperCase();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    /** Mask all but last 4 digits — useful when logging to shared log files. */
    private static String maskPhone(String p) {
        if (p == null || p.length() < 4) return "****";
        return "*".repeat(p.length() - 4) + p.substring(p.length() - 4);
    }
}
