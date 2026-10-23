package com.leela.urlshortener.domain;

import java.time.Instant;

public final class LinkGuards {

    private LinkGuards() {}

    public static boolean isRedirectable(Instant now, Instant expiresAt, Instant deletedAt) {
        if (deletedAt != null) {
            return false;
        }
        return expiresAt == null || expiresAt.isAfter(now);
    }
}
