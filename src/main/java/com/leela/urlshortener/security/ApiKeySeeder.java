package com.leela.urlshortener.security;

import com.leela.urlshortener.config.AppProperties;
import com.leela.urlshortener.domain.ApiKey;
import com.leela.urlshortener.persistence.ApiKeyRepository;
import com.leela.urlshortener.support.Hashes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ApiKeySeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ApiKeySeeder.class);

    private final AppProperties properties;
    private final ApiKeyRepository apiKeys;

    public ApiKeySeeder(AppProperties properties, ApiKeyRepository apiKeys) {
        this.properties = properties;
        this.apiKeys = apiKeys;
    }

    @Override
    public void run(ApplicationArguments args) {
        String raw = properties.apiKey();
        if (raw == null || raw.isBlank()) {
            log.warn("APP_API_KEY is empty; authenticated routes will reject all callers");
            return;
        }
        byte[] hash = Hashes.sha256(raw);
        if (apiKeys.findByKeyHash(hash).isPresent()) {
            return;
        }
        ApiKey key = new ApiKey();
        key.setName("local-dev");
        key.setKeyHash(hash);
        key.setKeyPrefix(raw.substring(0, Math.min(8, raw.length())));
        apiKeys.save(key);
        log.info("seeded API key prefix={}", key.getKeyPrefix());
    }
}
