package com.leela.urlshortener.persistence;

import com.leela.urlshortener.domain.IdempotencyRecord;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByApiKeyIdAndIdempotencyKey(UUID apiKeyId, String idempotencyKey);
}
