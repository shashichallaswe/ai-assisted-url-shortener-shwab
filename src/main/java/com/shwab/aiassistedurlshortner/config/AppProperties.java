package com.shwab.aiassistedurlshortner.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String baseUrl,
        String apiKey,
        String ipHashSalt,
        int codeLength,
        Duration cacheTtl,
        Duration negativeCacheTtl,
        RateLimit rateLimit) {

    public record RateLimit(int createPerMinute, int redirectPerMinute) {}
}
