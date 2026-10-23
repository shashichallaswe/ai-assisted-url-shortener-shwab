package com.shwab.aiassistedurlshortner.security;

import com.shwab.aiassistedurlshortner.domain.ApiKey;
import com.shwab.aiassistedurlshortner.persistence.ApiKeyRepository;
import com.shwab.aiassistedurlshortner.support.Hashes;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyAuthenticator {

    private final ApiKeyRepository apiKeys;

    public ApiKeyAuthenticator(ApiKeyRepository apiKeys) {
        this.apiKeys = apiKeys;
    }

    public Optional<ApiKey> authenticate(String presented) {
        if (presented == null || presented.isBlank()) {
            return Optional.empty();
        }
        return apiKeys.findByKeyHash(Hashes.sha256(presented)).filter(ApiKey::isActive);
    }
}
