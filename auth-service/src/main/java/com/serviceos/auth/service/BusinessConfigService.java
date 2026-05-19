package com.serviceos.auth.service;

import com.serviceos.auth.entity.BusinessConfig;
import com.serviceos.auth.repository.BusinessConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BusinessConfigService {

    private static final Logger log = LoggerFactory.getLogger(BusinessConfigService.class);

    private final BusinessConfigRepository repository;
    // In-memory cache for fast reads — refreshed on every write, valid indefinitely
    // (config changes are infrequent; services that need fresh values call REST)
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public BusinessConfigService(BusinessConfigRepository repository) {
        this.repository = repository;
        loadCache();
    }

    private void loadCache() {
        repository.findAll().forEach(c -> cache.put(c.getKey(), c.getValue()));
        log.info("BusinessConfig cache loaded: {} entries", cache.size());
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BusinessConfig> getAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<BusinessConfig> getEntry(String key) {
        return repository.findById(key);
    }

    /** Fast path for internal service-to-service calls. Returns null if key not found. */
    public String getValue(String key) {
        return cache.get(key);
    }

    public String getValue(String key, String defaultValue) {
        return cache.getOrDefault(key, defaultValue);
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    @Transactional
    public BusinessConfig set(String key, String value, String updatedBy) {
        BusinessConfig cfg = repository.findById(key)
                .orElseGet(() -> {
                    BusinessConfig c = new BusinessConfig();
                    c.setKey(key);
                    return c;
                });
        cfg.setValue(value);
        cfg.setUpdatedBy(updatedBy);
        cfg.setUpdatedAt(Instant.now());
        BusinessConfig saved = repository.save(cfg);
        cache.put(key, value);
        log.info("Config updated: key={} updatedBy={}", key, updatedBy);
        return saved;
    }
}
