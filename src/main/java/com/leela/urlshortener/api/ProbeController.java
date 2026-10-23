package com.leela.urlshortener.api;

import com.leela.urlshortener.api.dto.ErrorResponse;
import com.leela.urlshortener.support.ErrorCode;
import java.sql.Connection;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProbeController {

    private final DataSource dataSource;
    private final long startedAt = System.currentTimeMillis();

    public ProbeController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "ok", "uptimeSeconds", (System.currentTimeMillis() - startedAt) / 1000);
    }

    @GetMapping("/ready")
    public ResponseEntity<?> ready() {
        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().execute("SELECT 1");
            return ResponseEntity.ok(Map.of("status", "ready"));
        } catch (Exception e) {
            return ResponseEntity.status(503)
                    .body(ErrorResponse.of(ErrorCode.NOT_READY.name(), "postgres unavailable"));
        }
    }
}
