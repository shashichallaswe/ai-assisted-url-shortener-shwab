CREATE TABLE api_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    key_hash BYTEA NOT NULL UNIQUE,
    key_prefix TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at TIMESTAMPTZ
);

CREATE TABLE short_links (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code TEXT NOT NULL UNIQUE,
    original_url TEXT NOT NULL,
    created_by UUID NOT NULL REFERENCES api_keys (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT short_links_code_len CHECK (char_length(code) = 8),
    CONSTRAINT short_links_https CHECK (original_url LIKE 'https://%'),
    CONSTRAINT short_links_expiry CHECK (expires_at IS NULL OR expires_at > created_at)
);

CREATE INDEX short_links_expires_at_idx
    ON short_links (expires_at)
    WHERE expires_at IS NOT NULL AND deleted_at IS NULL;

CREATE TABLE click_events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    link_id BIGINT NOT NULL REFERENCES short_links (id),
    clicked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ip_hash BYTEA,
    user_agent TEXT,
    referer TEXT
);

CREATE INDEX click_events_link_clicked_idx
    ON click_events (link_id, clicked_at DESC);

CREATE TABLE idempotency_keys (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    api_key_id UUID NOT NULL REFERENCES api_keys (id),
    idempotency_key TEXT NOT NULL,
    request_hash BYTEA NOT NULL,
    link_id BIGINT REFERENCES short_links (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT idempotency_keys_unique UNIQUE (api_key_id, idempotency_key)
);
