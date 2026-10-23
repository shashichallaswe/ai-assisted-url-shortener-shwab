package com.shwab.aiassistedurlshortner.persistence;

import com.shwab.aiassistedurlshortner.domain.ApiKey;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    Optional<ApiKey> findByKeyHash(byte[] keyHash);
}
