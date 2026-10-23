package com.leela.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.leela.urlshortener.config.AppProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ShortCodeGeneratorTest {

    @Test
    void generatesEightCharacterCodesFromSafeAlphabet() {
        ShortCodeGenerator generator = new ShortCodeGenerator(properties());
        String code = generator.generate();
        assertThat(code).hasSize(8);
        assertThat(code.chars()).allMatch(ch -> ShortCodeGenerator.ALPHABET.indexOf(ch) >= 0);
        assertThat(generator.generate()).isNotEqualTo(code);
    }

    private static AppProperties properties() {
        return new AppProperties(
                "http://localhost:8080",
                "key",
                "salt",
                8,
                Duration.ofHours(1),
                Duration.ofSeconds(60),
                new AppProperties.RateLimit(30, 120));
    }
}
