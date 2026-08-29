-- liquibase formatted sql

-- changeset oojuniin:login_audit-v1-initial context:structure labels:api,login_audit
-- comment: Registra toda tentativa de login (sucesso ou falha) — username em texto porque tentativas com usuário inexistente também são auditadas.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'workbox' AND table_name = 'login_audit'
CREATE TABLE workbox.login_audit
(
    id         UUID PRIMARY KEY,
    username   VARCHAR(255) NOT NULL,
    successful BOOLEAN      NOT NULL,
    reason     VARCHAR(255),
    ip_address VARCHAR(64),
    created_at TIMESTAMP(6) NOT NULL
);

-- changeset oojuniin:login_audit-v1-index-username-created-at context:structure labels:api,login_audit
-- comment: Consulta típica é "últimas tentativas de um usuário" — índice composto sarga essa query.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'workbox' AND indexname = 'idx_login_audit_username_created_at'
CREATE INDEX idx_login_audit_username_created_at ON workbox.login_audit (username, created_at DESC);
