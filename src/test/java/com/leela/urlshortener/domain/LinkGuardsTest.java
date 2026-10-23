package com.leela.urlshortener.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class LinkGuardsTest {

    private final Instant now = Instant.parse("2026-08-23T22:00:00Z");

    @Test
    void liveLinkIsRedirectable() {
        assertThat(LinkGuards.isRedirectable(now, null, null)).isTrue();
    }

    @Test
    void deletedOrExpiredLinksAreNotRedirectable() {
        assertThat(LinkGuards.isRedirectable(now, null, now.minusSeconds(1))).isFalse();
        assertThat(LinkGuards.isRedirectable(now, now.minusSeconds(1), null)).isFalse();
        assertThat(LinkGuards.isRedirectable(now, now.plusSeconds(60), null)).isTrue();
    }
}
