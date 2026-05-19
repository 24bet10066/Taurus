package com.serviceos.auth.service;

import com.serviceos.auth.config.AuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Component
public class TwoFactorClient {

    private static final Logger log = LoggerFactory.getLogger(TwoFactorClient.class);

    private final WebClient webClient;
    private final AuthProperties props;

    public TwoFactorClient(WebClient.Builder builder, AuthProperties props) {
        this.props = props;
        this.webClient = builder.baseUrl(props.twofactor().baseUrl()).build();
    }

    public boolean sendOtp(String phone, String otp) {
        String apiKey = props.twofactor().apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[DEV] TWOFACTOR_API_KEY not set — OTP for {} is {}", phone, otp);
            return true;
        }
        try {
            Map<?, ?> response = webClient.get()
                    .uri("/{apiKey}/SMS/{phone}/{otp}", apiKey, phone, otp)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .onErrorResume(ex -> {
                        log.error("2Factor.in call failed for phone={}", phone, ex);
                        return Mono.empty();
                    })
                    .block();
            if (response == null) {
                return false;
            }
            Object status = response.get("Status");
            boolean ok = "Success".equalsIgnoreCase(String.valueOf(status));
            if (!ok) {
                log.warn("2Factor.in non-success: {}", response);
            }
            return ok;
        } catch (RuntimeException ex) {
            log.error("Unexpected 2Factor.in error for phone={}", phone, ex);
            return false;
        }
    }
}
