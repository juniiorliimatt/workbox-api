-- liquibase formatted sql

-- changeset oojuniin:password_reset_tokens-v1-initial context:structure labels:api,password_reset_tokens
-- comment: Token de recuperação de senha — só o hash é guardado (o token em si só existe no e-mail enviado ao usuário).
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'workbox' AND table_name = 'password_reset_tokens'
CREATE TABLE workbox.password_reset_tokens
(
    id         UUID PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES workbox.users_api (id) ON DELETE CASCADE,
    token_hash VARCHAR(64)  NOT NULL UNIQUE,
    expires_at TIMESTAMP(6) NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL
);

-- changeset oojuniin:password_reset_tokens-v1-index-user-id context:structure labels:api,password_reset_tokens
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'workbox' AND indexname = 'idx_password_reset_tokens_user_id'
CREATE INDEX idx_password_reset_tokens_user_id ON workbox.password_reset_tokens (user_id);
