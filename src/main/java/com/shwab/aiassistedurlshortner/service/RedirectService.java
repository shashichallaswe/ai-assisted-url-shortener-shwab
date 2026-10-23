package com.shwab.aiassistedurlshortner.service;

import com.shwab.aiassistedurlshortner.cache.CachedLink;
import com.shwab.aiassistedurlshortner.cache.LinkCache;
import com.shwab.aiassistedurlshortner.config.AppProperties;
import com.shwab.aiassistedurlshortner.domain.LinkGuards;
import com.shwab.aiassistedurlshortner.domain.ShortLink;
import com.shwab.aiassistedurlshortner.persistence.ShortLinkRepository;
import com.shwab.aiassistedurlshortner.security.AliasPolicy;
import com.shwab.aiassistedurlshortner.security.RateLimiter;
import com.shwab.aiassistedurlshortner.support.AppException;
import com.shwab.aiassistedurlshortner.support.ErrorCode;
import com.shwab.aiassistedurlshortner.support.Hashes;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RedirectService {

    private final ShortLinkRepository links;
    private final LinkCache cache;
    private final RateLimiter rateLimiter;
    private final ClickRecorder clicks;
    private final AppProperties properties;
    private final Clock clock;

    public RedirectService(
            ShortLinkRepository links,
            LinkCache cache,
            RateLimiter rateLimiter,
            ClickRecorder clicks,
            AppProperties properties,
            Clock clock) {
        this.links = links;
        this.cache = cache;
        this.rateLimiter = rateLimiter;
        this.clicks = clicks;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public String resolve(String code, String ip, String userAgent, String referer) {
        if (!AliasPolicy.looksLikeCode(code)) {
            throw notFound();
        }
        String ipKey = HexFormat.of().formatHex(Hashes.sha256(ip == null ? "unknown" : ip)).substring(0, 16);
        if (!rateLimiter.allowRedirect(ipKey)) {
            throw new AppException(ErrorCode.RATE_LIMITED, "Too many redirect requests");
        }

        Instant now = clock.instant();
        CachedLink cached = cache.get(code).orElse(null);
        if (cached != null) {
            if (cached.missing() || !LinkGuards.isRedirectable(now, cached.expiresAt(), cached.deletedAt())) {
                throw notFound();
            }
            clicks.record(cached.id(), ip, userAgent, referer);
            return cached.originalUrl();
        }

        ShortLink link = links.findByCode(code).orElse(null);
        if (link == null) {
            cache.put(code, CachedLink.absent(), properties.negativeCacheTtl());
            throw notFound();
        }
        cache.put(code, CachedLink.present(link.getId(), link.getOriginalUrl(), link.getExpiresAt(), link.getDeletedAt()), ttlFor(link, now));
        if (!LinkGuards.isRedirectable(now, link.getExpiresAt(), link.getDeletedAt())) {
            throw notFound();
        }
        clicks.record(link.getId(), ip, userAgent, referer);
        return link.getOriginalUrl();
    }

    private Duration ttlFor(ShortLink link, Instant now) {
        Duration ttl = properties.cacheTtl();
        if (link.getExpiresAt() != null) {
            Duration untilExpiry = Duration.between(now, link.getExpiresAt());
            if (untilExpiry.isNegative() || untilExpiry.isZero()) {
                return properties.negativeCacheTtl();
            }
            if (untilExpiry.compareTo(ttl) < 0) {
                return untilExpiry;
            }
        }
        return ttl;
    }

    private static AppException notFound() {
        return new AppException(ErrorCode.NOT_FOUND, "Short link not found");
    }
}
