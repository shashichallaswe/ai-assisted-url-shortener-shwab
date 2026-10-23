package com.shwab.aiassistedurlshortner.service;

import com.shwab.aiassistedurlshortner.config.AppProperties;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class ShortCodeGenerator {

    // Ambiguous characters 0/O, 1/I/l omitted so codes stay readable.
    static final String ALPHABET = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();
    private final int length;

    public ShortCodeGenerator(AppProperties properties) {
        this.length = properties.codeLength();
    }

    public String generate() {
        char[] buffer = new char[length];
        for (int i = 0; i < length; i++) {
            buffer[i] = ALPHABET.charAt(random.nextInt(ALPHABET.length()));
        }
        return new String(buffer);
    }
}
