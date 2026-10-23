package com.leela.urlshortener.api;

import com.leela.urlshortener.api.dto.ErrorResponse;
import com.leela.urlshortener.domain.ApiKey;
import com.leela.urlshortener.security.ApiKeyAuthenticator;
import com.leela.urlshortener.support.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String ATTR = "apiKey";

    private final ApiKeyAuthenticator authenticator;
    private final JsonMapper json = JsonMapper.shared();

    public ApiKeyFilter(ApiKeyAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        String token = null;
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = header.substring(7).trim();
        }
        Optional<ApiKey> key = authenticator.authenticate(token);
        if (key.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter()
                    .write(json.writeValueAsString(ErrorResponse.of(ErrorCode.UNAUTHORIZED.name(), "Invalid credentials")));
            return;
        }
        request.setAttribute(ATTR, key.get());
        chain.doFilter(request, response);
    }
}
