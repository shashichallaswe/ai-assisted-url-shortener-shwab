package com.shwab.aiassistedurlshortner.security;

import com.shwab.aiassistedurlshortner.config.AppProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);
    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyyMMddHHmm").withZone(ZoneOffset.UTC);

    private final StringRedisTemplate redis;
    private final AppProperties properties;
    private final Clock clock;

    public RateLimiter(StringRedisTemplate redis, AppProperties properties, Clock clock) {
        this.redis = redis;
        this.properties = properties;
        this.clock = clock;
    }

    public boolean allowCreate(String apiKeyId) {
        return allow("rl:create:" + apiKeyId, properties.rateLimit().createPerMinute(), false);
    }

    public boolean allowRedirect(String ipHashPrefix) {
        return allow("rl:redirect:" + ipHashPrefix, properties.rateLimit().redirectPerMinute(), true);
    }

    private boolean allow(String prefix, int limit, boolean failOpen) {
        String key = prefix + ":" + MINUTE.format(Instant.now(clock));
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redis.expire(key, java.time.Duration.ofSeconds(70));
            }
            return count == null || count <= limit;
        } catch (RuntimeException e) {
            log.warn("rate limiter unavailable, failOpen={}", failOpen, e);
            return failOpen;
        }
    }
}
