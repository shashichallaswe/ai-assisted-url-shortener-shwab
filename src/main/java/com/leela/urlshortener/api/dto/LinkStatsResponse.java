package com.leela.urlshortener.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record LinkStatsResponse(
        String code, long totalClicks, Instant lastClickedAt, List<DayCount> clicksByDay) {

    public record DayCount(LocalDate date, long count) {}
}
