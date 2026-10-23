package com.shwab.aiassistedurlshortner.service;

import com.shwab.aiassistedurlshortner.api.dto.LinkStatsResponse;
import com.shwab.aiassistedurlshortner.domain.ShortLink;
import com.shwab.aiassistedurlshortner.persistence.ClickEventRepository;
import com.shwab.aiassistedurlshortner.persistence.ShortLinkRepository;
import com.shwab.aiassistedurlshortner.support.AppException;
import com.shwab.aiassistedurlshortner.support.ErrorCode;
import java.sql.Date;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatsService {

    private final ShortLinkRepository links;
    private final ClickEventRepository clicks;
    private final Clock clock;

    public StatsService(ShortLinkRepository links, ClickEventRepository clicks, Clock clock) {
        this.links = links;
        this.clicks = clicks;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public LinkStatsResponse stats(String code) {
        ShortLink link = links.findByCode(code).orElseThrow(() -> notFound());
        if (link.getDeletedAt() != null) {
            throw notFound();
        }
        long total = clicks.countByLinkId(link.getId());
        var last = clicks.findTopByLinkIdOrderByClickedAtDesc(link.getId()).orElse(null);
        List<LinkStatsResponse.DayCount> byDay = clicks.dailyCounts(link.getId(), clock.instant().minus(Duration.ofDays(30))).stream()
                .map(row -> new LinkStatsResponse.DayCount(toLocalDate(row[0]), ((Number) row[1]).longValue()))
                .toList();
        return new LinkStatsResponse(link.getCode(), total, last == null ? null : last.getClickedAt(), byDay);
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        return LocalDate.parse(value.toString());
    }

    private static AppException notFound() {
        return new AppException(ErrorCode.NOT_FOUND, "Short link not found");
    }
}
