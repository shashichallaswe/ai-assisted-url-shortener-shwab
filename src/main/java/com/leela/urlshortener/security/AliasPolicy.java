package com.leela.urlshortener.security;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class AliasPolicy {

    private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9_-]{4,32}$");
    private static final Set<String> RESERVED = Set.of(
            "api",
            "health",
            "ready",
            "actuator",
            "links",
            "docs",
            "swagger",
            "favicon",
            "error");

    private AliasPolicy() {}

    public static String validate(String alias) {
        if (alias == null || alias.isBlank()) {
            throw new IllegalArgumentException("alias is required");
        }
        String trimmed = alias.trim();
        if (!VALID.matcher(trimmed).matches()) {
            return "alias must be 4-32 characters of letters, digits, underscore, or hyphen";
        }
        if (RESERVED.contains(trimmed.toLowerCase(Locale.ROOT))) {
            return "alias is reserved";
        }
        return null;
    }

    public static boolean looksLikeCode(String value) {
        return value != null && VALID.matcher(value).matches();
    }
}
