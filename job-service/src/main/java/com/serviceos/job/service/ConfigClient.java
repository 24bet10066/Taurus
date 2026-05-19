package com.serviceos.job.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads dynamic business config values from auth-service's internal endpoint.
 * Caches values locally for 5 minutes to avoid per-request HTTP calls.
 * Fails open: returns provided default if auth-service is unavailable.
 */
@Component
public class ConfigClient {

    private static final Logger log = LoggerFactory.getLogger(ConfigClient.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final RestClient restClient;
    private final Map<String, CachedValue> cache = new ConcurrentHashMap<>();

    public ConfigClient(@Value("${services.auth.url:http://localhost:8081}") String authUrl) {
        this.restClient = RestClient.builder().baseUrl(authUrl).build();
    }

    public String get(String key, String defaultValue) {
        CachedValue cached = cache.get(key);
        if (cached != null && !cached.isExpired()) {
            return cached.value;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> response = restClient.get()
                    .uri("/internal/config/" + key)
                    .retrieve()
                    .body(Map.class);
            String value = response != null ? response.get("value") : null;
            if (value != null) {
                cache.put(key, new CachedValue(value, Instant.now()));
                return value;
            }
        } catch (Exception ex) {
            log.warn("Config fetch failed for key={}: {}", key, ex.getMessage());
        }
        return cached != null ? cached.value : defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        try { return Integer.parseInt(get(key, String.valueOf(defaultValue))); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    private static class CachedValue {
        final String value;
        final Instant fetchedAt;
        CachedValue(String value, Instant fetchedAt) { this.value = value; this.fetchedAt = fetchedAt; }
        boolean isExpired() { return Instant.now().isAfter(fetchedAt.plus(CACHE_TTL)); }
    }
}
