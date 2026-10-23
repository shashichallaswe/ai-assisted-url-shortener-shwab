package com.leela.urlshortener.api;

import com.leela.urlshortener.api.dto.CreateLinkRequest;
import com.leela.urlshortener.api.dto.LinkResponse;
import com.leela.urlshortener.api.dto.LinkStatsResponse;
import com.leela.urlshortener.domain.ApiKey;
import com.leela.urlshortener.service.LinkService;
import com.leela.urlshortener.service.StatsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/links")
public class LinkController {

    private final LinkService links;
    private final StatsService stats;

    public LinkController(LinkService links, StatsService stats) {
        this.links = links;
        this.stats = stats;
    }

    @PostMapping
    public ResponseEntity<LinkResponse> create(
            HttpServletRequest request,
            @Valid @RequestBody CreateLinkRequest body,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        ApiKey caller = (ApiKey) request.getAttribute(ApiKeyFilter.ATTR);
        LinkResponse created = links.create(caller, body, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, "/api/v1/links/" + created.code())
                .body(created);
    }

    @GetMapping("/{code}")
    public LinkResponse get(@PathVariable String code) {
        return links.get(code);
    }

    @GetMapping("/{code}/stats")
    public LinkStatsResponse stats(@PathVariable String code) {
        return stats.stats(code);
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        links.delete(code);
        return ResponseEntity.noContent().build();
    }
}
