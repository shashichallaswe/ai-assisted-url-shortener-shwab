package com.shwab.aiassistedurlshortner.security;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Structural checks only. The service never fetches, resolves, or crawls a destination.
 */
public final class DestinationPolicy {

    public static final int MAX_LENGTH = 2048;
    private static final Pattern IPV4 = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");

    private DestinationPolicy() {}

    public static Optional<String> rejection(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.of("url is required");
        }
        if (raw.length() > MAX_LENGTH) {
            return Optional.of("url exceeds 2048 characters");
        }
        if (raw.chars().anyMatch(Character::isWhitespace)) {
            return Optional.of("url must not contain whitespace");
        }

        final URI uri;
        try {
            uri = URI.create(raw);
        } catch (IllegalArgumentException e) {
            return Optional.of("url is not a valid URI");
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            return Optional.of("only https URLs are allowed");
        }
        if (uri.getRawUserInfo() != null) {
            return Optional.of("url must not contain credentials");
        }
        String host = hostOf(uri);
        if (host == null || host.isBlank()) {
            return Optional.of("url must include a host");
        }
        if (isLocalOrPrivate(host)) {
            return Optional.of("url must not target a private or local address");
        }
        return Optional.empty();
    }

    static String hostOf(URI uri) {
        String host = uri.getHost();
        if (host == null) {
            return null;
        }
        if (host.startsWith("[") && host.endsWith("]")) {
            return host.substring(1, host.length() - 1).toLowerCase(Locale.ROOT);
        }
        return host.toLowerCase(Locale.ROOT);
    }

    static boolean isLocalOrPrivate(String host) {
        if ("localhost".equals(host) || host.endsWith(".localhost") || "localhost.localdomain".equals(host)) {
            return true;
        }
        if ("::1".equals(host) || host.startsWith("fe80:") || host.startsWith("fc") || host.startsWith("fd")) {
            return true;
        }
        var matcher = IPV4.matcher(host);
        if (!matcher.matches()) {
            return false;
        }
        int a = Integer.parseInt(matcher.group(1));
        int b = Integer.parseInt(matcher.group(2));
        int c = Integer.parseInt(matcher.group(3));
        int d = Integer.parseInt(matcher.group(4));
        if (a > 255 || b > 255 || c > 255 || d > 255) {
            return true;
        }
        if (a == 127 || a == 0 || a == 10 || a == 169 && b == 254) {
            return true;
        }
        if (a == 192 && b == 168) {
            return true;
        }
        return a == 172 && b >= 16 && b <= 31;
    }
}
