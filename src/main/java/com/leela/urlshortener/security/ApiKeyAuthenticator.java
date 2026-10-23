package com.leela.urlshortener.security;

import com.leela.urlshortener.domain.ApiKey;
import com.leela.urlshortener.persistence.ApiKeyRepository;
import com.leela.urlshortener.support.Hashes;
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
