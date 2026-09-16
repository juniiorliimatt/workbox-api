-- liquibase formatted sql

-- changeset oojuniin:refresh-tokens-v2-drop-migrated-to-redis context:structure labels:auth,refresh-tokens
-- comment: refresh_tokens migrou de Postgres para Redis (RefreshTokenService) - TTL nativo por chave substitui o cleanup job agendado, e o script Lua de consumo atomico substitui a leitura+escrita em duas chamadas JPA separadas. Tabela nao tem mais leitor/escritor no codigo, dropada.
DROP TABLE IF EXISTS workbox.refresh_tokens;
-- rollback CREATE TABLE workbox.refresh_tokens (id UUID PRIMARY KEY, jti UUID NOT NULL UNIQUE, family_id UUID NOT NULL, user_id UUID NOT NULL, issued_at TIMESTAMP NOT NULL, expires_at TIMESTAMP NOT NULL, revoked_at TIMESTAMP);
