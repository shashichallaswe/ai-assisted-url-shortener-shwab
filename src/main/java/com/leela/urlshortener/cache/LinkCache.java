package com.leela.urlshortener.cache;

import java.time.Duration;
import java.util.Optional;

public interface LinkCache {

    Optional<CachedLink> get(String code);

    void put(String code, CachedLink link, Duration ttl);

    boolean evict(String code);
}
