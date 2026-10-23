package com.shwab.aiassistedurlshortner.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateLinkRequest(
        @NotBlank @Size(max = 2048) String originalUrl,
        Instant expiresAt,
        @Size(max = 32) String alias) {}
