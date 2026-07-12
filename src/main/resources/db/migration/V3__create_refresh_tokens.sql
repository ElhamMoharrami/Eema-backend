CREATE TABLE refresh_tokens
(
    id         UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash TEXT        NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX refresh_tokens_hash_idx
    ON refresh_tokens (token_hash);

CREATE INDEX refresh_tokens_user_idx
    ON refresh_tokens (user_id);

CREATE INDEX refresh_tokens_expiry_idx
    ON refresh_tokens (expires_at) WHERE NOT revoked;

-- SHA-256 hash of the refresh token.
-- Never store the raw refresh token.