package com.leela.urlshortener.service;

import com.leela.urlshortener.api.dto.CreateLinkRequest;
import com.leela.urlshortener.api.dto.LinkResponse;
import com.leela.urlshortener.cache.CachedLink;
import com.leela.urlshortener.cache.LinkCache;
import com.leela.urlshortener.config.AppProperties;
import com.leela.urlshortener.domain.ApiKey;
import com.leela.urlshortener.domain.IdempotencyRecord;
import com.leela.urlshortener.domain.ShortLink;
import com.leela.urlshortener.persistence.IdempotencyRepository;
import com.leela.urlshortener.persistence.ShortLinkRepository;
import com.leela.urlshortener.security.AliasPolicy;
import com.leela.urlshortener.security.DestinationPolicy;
import com.leela.urlshortener.security.RateLimiter;
import com.leela.urlshortener.support.AppException;
import com.leela.urlshortener.support.ErrorCode;
import com.leela.urlshortener.support.Hashes;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LinkService {

    private static final int MAX_CODE_ATTEMPTS = 3;

    private final ShortLinkRepository links;
    private final IdempotencyRepository idempotency;
    private final ShortCodeGenerator codes;
    private final LinkCache cache;
    private final RateLimiter rateLimiter;
    private final AppProperties properties;
    private final Clock clock;

    public LinkService(
            ShortLinkRepository links,
            IdempotencyRepository idempotency,
            ShortCodeGenerator codes,
            LinkCache cache,
            RateLimiter rateLimiter,
            AppProperties properties,
            Clock clock) {
        this.links = links;
        this.idempotency = idempotency;
        this.codes = codes;
        this.cache = cache;
        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public LinkResponse create(ApiKey caller, CreateLinkRequest request, String idempotencyKey) {
        if (!rateLimiter.allowCreate(caller.getId().toString())) {
            throw new AppException(ErrorCode.RATE_LIMITED, "Too many create requests");
        }

        DestinationPolicy.rejection(request.originalUrl())
                .ifPresent(reason -> {
                    throw new AppException(
                            ErrorCode.VALIDATION_ERROR,
                            "Invalid destination",
                            List.of(new AppException.FieldError("originalUrl", reason)));
                });

        Instant now = clock.instant();
        if (request.expiresAt() != null && !request.expiresAt().isAfter(now)) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "expiresAt must be in the future",
                    List.of(new AppException.FieldError("expiresAt", "must be in the future")));
        }

        String alias = blankToNull(request.alias());
        if (alias != null) {
            String aliasIssue = AliasPolicy.validate(alias);
            if (aliasIssue != null) {
                throw new AppException(
                        ErrorCode.VALIDATION_ERROR,
                        "Invalid alias",
                        List.of(new AppException.FieldError("alias", aliasIssue)));
            }
        }

        byte[] fingerprint = Hashes.sha256(request.originalUrl() + "|" + request.expiresAt() + "|" + alias);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = idempotency.findByApiKeyIdAndIdempotencyKey(caller.getId(), idempotencyKey);
            if (existing.isPresent()) {
                IdempotencyRecord record = existing.get();
                if (!Hashes.equal(record.getRequestHash(), fingerprint)) {
                    throw new AppException(ErrorCode.CONFLICT, "Idempotency-Key was reused with a different body");
                }
                ShortLink prior = links.findById(record.getLinkId()).orElseThrow();
                return toResponse(prior);
            }
        }

        ShortLink saved = alias == null ? insertGenerated(caller, request) : insertAlias(caller, request, alias);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            try {
                IdempotencyRecord record = new IdempotencyRecord();
                record.setApiKeyId(caller.getId());
                record.setIdempotencyKey(idempotencyKey);
                record.setRequestHash(fingerprint);
                record.setLinkId(saved.getId());
                idempotency.save(record);
            } catch (DataIntegrityViolationException e) {
                IdempotencyRecord record = idempotency
                        .findByApiKeyIdAndIdempotencyKey(caller.getId(), idempotencyKey)
                        .orElseThrow();
                if (!Hashes.equal(record.getRequestHash(), fingerprint)) {
                    throw new AppException(ErrorCode.CONFLICT, "Idempotency-Key was reused with a different body");
                }
                return toResponse(links.findById(record.getLinkId()).orElseThrow());
            }
        }
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public LinkResponse get(String code) {
        ShortLink link = links.findByCode(code).orElseThrow(() -> notFound());
        if (link.getDeletedAt() != null) {
            throw notFound();
        }
        return toResponse(link);
    }

    @Transactional
    public void delete(String code) {
        ShortLink link = links.findByCode(code).orElseThrow(() -> notFound());
        if (link.getDeletedAt() != null) {
            throw notFound();
        }
        link.setDeletedAt(clock.instant());
        links.save(link);
        if (!cache.evict(code)) {
            throw new AppException(ErrorCode.CACHE_INVALIDATION_FAILED, "Link deleted but cache invalidation failed");
        }
        cache.put(code, CachedLink.absent(), properties.negativeCacheTtl());
    }

    private ShortLink insertAlias(ApiKey caller, CreateLinkRequest request, String alias) {
        ShortLink link = newLink(caller, request, alias, true);
        try {
            return links.saveAndFlush(link);
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.CONFLICT, "Alias is already in use");
        }
    }

    private ShortLink insertGenerated(ApiKey caller, CreateLinkRequest request) {
        DataIntegrityViolationException last = null;
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            ShortLink link = newLink(caller, request, codes.generate(), false);
            try {
                return links.saveAndFlush(link);
            } catch (DataIntegrityViolationException e) {
                last = e;
            }
        }
        throw new AppException(ErrorCode.CODE_EXHAUSTED, "Unable to allocate a unique short code", last);
    }

    private ShortLink newLink(ApiKey caller, CreateLinkRequest request, String code, boolean customAlias) {
        ShortLink link = new ShortLink();
        link.setCode(code);
        link.setOriginalUrl(request.originalUrl());
        link.setCreatedBy(caller.getId());
        link.setExpiresAt(request.expiresAt());
        link.setCustomAlias(customAlias);
        return link;
    }

    private LinkResponse toResponse(ShortLink link) {
        String base = properties.baseUrl().endsWith("/")
                ? properties.baseUrl().substring(0, properties.baseUrl().length() - 1)
                : properties.baseUrl();
        return new LinkResponse(
                link.getCode(),
                base + "/" + link.getCode(),
                link.getOriginalUrl(),
                link.getCreatedAt(),
                link.getExpiresAt(),
                link.isCustomAlias());
    }

    private static AppException notFound() {
        return new AppException(ErrorCode.NOT_FOUND, "Short link not found");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
