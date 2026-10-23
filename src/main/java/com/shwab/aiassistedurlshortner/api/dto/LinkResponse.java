package com.shwab.aiassistedurlshortner.api.dto;

import java.time.Instant;

public record LinkResponse(
        String code,
        String shortUrl,
        String originalUrl,
        Instant createdAt,
        Instant expiresAt,
        boolean customAlias) {}
