package com.shwab.aiassistedurlshortner.cache;

import com.shwab.aiassistedurlshortner.config.AppProperties;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class RedisLinkCache implements LinkCache {

    private static final Logger log = LoggerFactory.getLogger(RedisLinkCache.class);
    private static final String PREFIX = "link:v1:";

    private final StringRedisTemplate redis;
    private final AppProperties properties;
    private final JsonMapper json = JsonMapper.shared();

    public RedisLinkCache(StringRedisTemplate redis, AppProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    @Override
    public Optional<CachedLink> get(String code) {
        try {
            String raw = redis.opsForValue().get(PREFIX + code);
            if (raw == null) {
                return Optional.empty();
            }
            return Optional.of(json.readValue(raw, CachedLink.class));
        } catch (RuntimeException e) {
            log.warn("cache get failed for code={}", code, e);
            return Optional.empty();
        }
    }

    @Override
    public void put(String code, CachedLink link, Duration ttl) {
        try {
            redis.opsForValue().set(PREFIX + code, json.writeValueAsString(link), ttl);
        } catch (RuntimeException e) {
            log.warn("cache put failed for code={}", code, e);
        }
    }

    @Override
    public boolean evict(String code) {
        try {
            redis.delete(PREFIX + code);
            return true;
        } catch (RuntimeException e) {
            log.error("cache evict failed for code={}", code, e);
            return false;
        }
    }

    public Duration negativeTtl() {
        return properties.negativeCacheTtl();
    }
}
