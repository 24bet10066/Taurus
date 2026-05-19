package com.serviceos.notification.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class WhatsAppChannel {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppChannel.class);

    private final RestClient restClient;

    public WhatsAppChannel(
            @Value("${serviceos.whatsapp.base-url}") String baseUrl,
            @Value("${serviceos.whatsapp.phone-number-id:}") String phoneNumberId,
            @Value("${serviceos.whatsapp.access-token:}") String accessToken) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl + "/" + phoneNumberId + "/messages")
                .defaultHeader("Authorization", "Bearer " + accessToken)
                .build();
    }

    public boolean send(String toPhone, String message) {
        if (toPhone == null || toPhone.isBlank()) {
            log.warn("WhatsApp send skipped — no phone number");
            return false;
        }
        String phone = normalise(toPhone);
        try {
            var body = Map.of(
                    "messaging_product", "whatsapp",
                    "recipient_type", "individual",
                    "to", phone,
                    "type", "text",
                    "text", Map.of("preview_url", false, "body", message)
            );
            restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[WA] sent to={} preview='{}'", phone, preview(message));
            return true;
        } catch (Exception ex) {
            log.warn("[WA] failed to={} reason={}", phone, ex.getMessage());
            return false;
        }
    }

    private static String normalise(String phone) {
        String digits = phone.replaceAll("\\D", "");
        return digits.length() == 10 ? "91" + digits : digits;
    }

    private static String preview(String msg) {
        return msg.length() <= 40 ? msg : msg.substring(0, 40) + "…";
    }
}
