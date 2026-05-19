package com.serviceos.notification.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class SmsChannel {

    private static final Logger log = LoggerFactory.getLogger(SmsChannel.class);
    private static final int SMS_MAX_CHARS = 160;

    private final RestClient restClient;
    private final String apiKey;

    public SmsChannel(
            @Value("${serviceos.sms.fast2sms.base-url}") String baseUrl,
            @Value("${serviceos.sms.fast2sms.api-key:}") String apiKey) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    public boolean send(String toPhone, String message) {
        if (toPhone == null || toPhone.isBlank()) {
            log.warn("SMS send skipped — no phone number");
            return false;
        }
        String phone = toPhone.replaceAll("\\D", "");
        if (phone.startsWith("91") && phone.length() == 12) phone = phone.substring(2);
        String truncated = message.length() > SMS_MAX_CHARS
                ? message.substring(0, SMS_MAX_CHARS - 1) + "…"
                : message;
        try {
            String uri = UriComponentsBuilder.newInstance()
                    .queryParam("authorization", apiKey)
                    .queryParam("route", "q")
                    .queryParam("message", truncated)
                    .queryParam("language", "unicode")
                    .queryParam("flash", "0")
                    .queryParam("numbers", phone)
                    .build().toUriString();

            restClient.get()
                    .uri(uri)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[SMS] sent to={} chars={}", phone, truncated.length());
            return true;
        } catch (Exception ex) {
            log.warn("[SMS] failed to={} reason={}", phone, ex.getMessage());
            return false;
        }
    }
}
