package com.leela.urlshortener.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AliasPolicyTest {

    @Test
    void acceptsReadableAlias() {
        assertThat(AliasPolicy.validate("launch-day")).isNull();
    }

    @Test
    void rejectsReservedAndMalformedAliases() {
        assertThat(AliasPolicy.validate("api")).isNotNull();
        assertThat(AliasPolicy.validate("ab")).isNotNull();
        assertThat(AliasPolicy.validate("has/slash")).isNotNull();
    }

    @Test
    void looksLikeCodeAcceptsGeneratedShape() {
        assertThat(AliasPolicy.looksLikeCode("launch-day")).isTrue();
        assertThat(AliasPolicy.looksLikeCode("ab")).isFalse();
    }
}
