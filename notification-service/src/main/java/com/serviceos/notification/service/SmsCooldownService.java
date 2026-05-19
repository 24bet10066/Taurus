package com.serviceos.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Prevents duplicate SMS to the same phone+messageType within a configurable window.
 * WhatsApp has NO cooldown — this service is SMS-only.
 * Admin/shop phone bypasses cooldown on all channels.
 */
@Service
public class SmsCooldownService {

    private static final Logger log = LoggerFactory.getLogger(SmsCooldownService.class);
    private static final String KEY_PREFIX = "sms:cooldown:";

    private final StringRedisTemplate redis;
    private final String shopPhone;
    private final long cooldownHours;

    public SmsCooldownService(
            StringRedisTemplate redis,
            @Value("${serviceos.notification.shop-phone:}") String shopPhone,
            @Value("${serviceos.notification.sms-cooldown-hours:24}") long cooldownHours) {
        this.redis = redis;
        this.shopPhone = normalise(shopPhone);
        this.cooldownHours = cooldownHours;
    }

    /**
     * Returns true if an SMS SHOULD be sent (no cooldown, or cooldown expired, or bypassed).
     */
    public boolean shouldSend(String toPhone, String messageType, boolean emergencyOverride) {
        String phone = normalise(toPhone);

        // Admin phone: never suppress
        if (!shopPhone.isEmpty() && shopPhone.equals(phone)) {
            return true;
        }
        // Emergency override: always send
        if (emergencyOverride) {
            log.debug("SMS emergency override for phone={} type={}", phone, messageType);
            return true;
        }

        String key = KEY_PREFIX + phone + ":" + messageType;
        try {
            Boolean exists = redis.hasKey(key);
            if (Boolean.TRUE.equals(exists)) {
                log.info("SMS suppressed (cooldown active) phone={} type={}", phone, messageType);
                return false;
            }
            return true;
        } catch (Exception ex) {
            // Redis unavailable → fail open (send the SMS)
            log.warn("Redis unavailable for cooldown check — sending SMS anyway: {}", ex.getMessage());
            return true;
        }
    }

    /**
     * Records that an SMS was successfully sent; starts the cooldown timer.
     */
    public void recordSent(String toPhone, String messageType) {
        String phone = normalise(toPhone);
        if (shopPhone.equals(phone)) return; // Admin phone has no cooldown timer
        String key = KEY_PREFIX + phone + ":" + messageType;
        try {
            redis.opsForValue().set(key, "1", cooldownHours, TimeUnit.HOURS);
        } catch (Exception ex) {
            log.warn("Failed to record SMS cooldown in Redis: {}", ex.getMessage());
        }
    }

    private static String normalise(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("\\D", "");
        if (digits.startsWith("91") && digits.length() == 12) digits = digits.substring(2);
        return digits;
    }
}
