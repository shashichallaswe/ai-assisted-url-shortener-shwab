package com.leela.urlshortener.service;

import com.leela.urlshortener.config.AppProperties;
import com.leela.urlshortener.domain.ClickEvent;
import com.leela.urlshortener.persistence.ClickEventRepository;
import com.leela.urlshortener.support.Hashes;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClickRecorder {

    private static final Logger log = LoggerFactory.getLogger(ClickRecorder.class);

    private final ClickEventRepository clicks;
    private final AppProperties properties;
    private final Clock clock;

    public ClickRecorder(ClickEventRepository clicks, AppProperties properties, Clock clock) {
        this.clicks = clicks;
        this.properties = properties;
        this.clock = clock;
    }

    @Async("clickExecutor")
    @Transactional
    public void record(long linkId, String ip, String userAgent, String referer) {
        try {
            ClickEvent event = new ClickEvent();
            event.setLinkId(linkId);
            event.setClickedAt(clock.instant());
            if (ip != null && !ip.isBlank()) {
                event.setIpHash(Hashes.sha256(properties.ipHashSalt() + "|" + ip));
            }
            event.setUserAgent(truncate(userAgent, 512));
            event.setReferer(truncate(referer, 512));
            clicks.save(event);
        } catch (RuntimeException e) {
            log.warn("click persist failed for linkId={}", linkId, e);
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
