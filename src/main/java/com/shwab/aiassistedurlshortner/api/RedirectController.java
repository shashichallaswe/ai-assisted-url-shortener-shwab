package com.shwab.aiassistedurlshortner.api;

import com.shwab.aiassistedurlshortner.service.RedirectService;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {

    private final RedirectService redirects;

    public RedirectController(RedirectService redirects) {
        this.redirects = redirects;
    }

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(
            @PathVariable String code, jakarta.servlet.http.HttpServletRequest request) {
        String destination = redirects.resolve(
                code,
                request.getRemoteAddr(),
                request.getHeader("User-Agent"),
                request.getHeader("Referer"));
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(destination))
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .build();
    }
}
