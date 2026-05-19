package com.serviceos.notification.channel;

import com.serviceos.notification.service.SmsCooldownService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Channel priority: WhatsApp (no cooldown, always first) → SMS (24hr cooldown per phone+type).
 * Admin/shop phone bypasses SMS cooldown. emergencyOverride bypasses everything.
 */
@Component
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final WhatsAppChannel whatsApp;
    private final SmsChannel sms;
    private final SmsCooldownService cooldown;

    public NotificationDispatcher(WhatsAppChannel whatsApp, SmsChannel sms,
                                   SmsCooldownService cooldown) {
        this.whatsApp = whatsApp;
        this.sms = sms;
        this.cooldown = cooldown;
    }

    /**
     * Try WhatsApp (no cooldown); fall back to SMS with 24hr deduplication.
     *
     * @param messageType one of: JOB_CREATED, JOB_ASSIGNED, JOB_COMPLETED,
     *                    JOB_CANCELLED, CREDIT_ALERT, REORDER_ALERT, etc.
     * @param emergency   if true, SMS cooldown is bypassed
     */
    public SendResult dispatch(String phone, String message, String messageType, boolean emergency) {
        // 1. WhatsApp — always try, no cooldown
        if (whatsApp.send(phone, message)) {
            return SendResult.ok("whatsapp");
        }
        log.info("WA failed — falling back to SMS for phone={} type={}", phone, messageType);

        // 2. SMS — apply cooldown check
        if (!cooldown.shouldSend(phone, messageType, emergency)) {
            return SendResult.suppressed("sms_cooldown");
        }
        if (sms.send(phone, message)) {
            cooldown.recordSent(phone, messageType);
            return SendResult.ok("sms");
        }
        log.warn("All channels failed for phone={}", phone);
        return SendResult.fail("all channels failed");
    }

    /**
     * Send on both channels independently (e.g. CANCELLED — customer AND admin).
     * SMS cooldown still applies per channel.
     */
    public void dispatchBoth(String phone, String message, String messageType, boolean emergency) {
        boolean waSent = whatsApp.send(phone, message);
        if (!waSent && cooldown.shouldSend(phone, messageType, emergency)) {
            boolean smsSent = sms.send(phone, message);
            if (smsSent) cooldown.recordSent(phone, messageType);
            if (!smsSent) log.warn("Both channels failed for phone={}", phone);
        }
    }

    /** Legacy convenience — no cooldown context (used by admin-only paths). */
    public SendResult dispatchAdmin(String phone, String message) {
        if (whatsApp.send(phone, message)) return SendResult.ok("whatsapp");
        if (sms.send(phone, message))     return SendResult.ok("sms");
        return SendResult.fail("all channels failed");
    }
}
