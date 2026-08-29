-- liquibase formatted sql

-- changeset oojuniin:refresh_tokens-v1-initial context:structure labels:api,refresh_tokens
-- comment: Rastreamento de refresh tokens por família de rotação — jti identifica o token emitido, family_id agrupa toda a cadeia originada de um mesmo login (usado pra detectar reuso de token roubado).
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'workbox' AND table_name = 'refresh_tokens'
CREATE TABLE workbox.refresh_tokens
(
    id         UUID PRIMARY KEY,
    jti        UUID         NOT NULL UNIQUE,
    family_id  UUID         NOT NULL,
    user_id    UUID         NOT NULL REFERENCES workbox.users_api (id) ON DELETE CASCADE,
    issued_at  TIMESTAMP(6) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    revoked_at TIMESTAMP(6)
);

-- changeset oojuniin:refresh_tokens-v1-index-family context:structure labels:api,refresh_tokens
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'workbox' AND indexname = 'idx_refresh_tokens_family_id'
CREATE INDEX idx_refresh_tokens_family_id ON workbox.refresh_tokens (family_id);

-- changeset oojuniin:refresh_tokens-v1-index-expires-at context:structure labels:api,refresh_tokens
-- comment: Suporta a limpeza periódica (RefreshTokenCleanupJob) de tokens expirados.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'workbox' AND indexname = 'idx_refresh_tokens_expires_at'
CREATE INDEX idx_refresh_tokens_expires_at ON workbox.refresh_tokens (expires_at);
