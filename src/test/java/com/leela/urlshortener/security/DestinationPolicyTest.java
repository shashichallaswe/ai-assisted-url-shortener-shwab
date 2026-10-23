package com.leela.urlshortener.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DestinationPolicyTest {

    @Test
    void acceptsPublicHttpsUrl() {
        assertThat(DestinationPolicy.rejection("https://example.com/path?q=1")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "http://example.com",
                "javascript:alert(1)",
                "https://localhost/secret",
                "https://127.0.0.1/",
                "https://192.168.1.20/admin",
                "https://10.0.0.4/",
                "https://172.16.0.8/",
                "https://user:pass@example.com",
                "not-a-url",
                "https://example.com/has space"
            })
    void rejectsUnsafeDestinations(String url) {
        assertThat(DestinationPolicy.rejection(url)).isPresent();
    }
}
