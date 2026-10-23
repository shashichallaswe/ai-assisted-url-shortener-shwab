package com.shwab.aiassistedurlshortner.cache;

import java.time.Instant;

public record CachedLink(long id, String originalUrl, Instant expiresAt, Instant deletedAt, boolean missing) {

    public static CachedLink present(long id, String originalUrl, Instant expiresAt, Instant deletedAt) {
        return new CachedLink(id, originalUrl, expiresAt, deletedAt, false);
    }

    public static CachedLink absent() {
        return new CachedLink(-1, "", null, null, true);
    }
}
