-- liquibase formatted sql

-- changeset oojuniin:api_clients-v1-initial context:structure labels:api,api_clients
-- comment: Clientes de servico-a-servico (ex.: budget-service) autorizados a consultar POST /api/v1/auth/introspect. Cadastro e 100% dado: liberar um microservico novo e inserir uma linha aqui via changeset novo, nunca editar SecurityConfig.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'workbox' AND table_name = 'api_clients'
CREATE TABLE workbox.api_clients
(
    id                  UUID         PRIMARY KEY,
    name                VARCHAR(80)  NOT NULL,
    client_id           VARCHAR(80)  NOT NULL,
    client_secret_hash  VARCHAR(100) NOT NULL,
    allowed_grant_types JSONB        NOT NULL DEFAULT '["CLIENT_SECRET"]',
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP(6) NOT NULL,
    updated_at          TIMESTAMP(6),
    created_by          VARCHAR(120),
    updated_by          VARCHAR(120),
    CONSTRAINT uk_api_clients_name UNIQUE (name),
    CONSTRAINT uk_api_clients_client_id UNIQUE (client_id)
);

-- changeset oojuniin:api_clients-v1-seed-budget-service context:data labels:api,api_clients
-- comment: Cliente inicial do budget-service, secret de estudo local (ver .env.example na raiz do monorepo). Em producao, gere um secret forte e insira o cliente real via changeset novo (nunca edite este arquivo ja aplicado) - ver README do workbox-api, secao "Clientes de introspeccao".
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM workbox.api_clients WHERE client_id = 'budget-service'
INSERT INTO workbox.api_clients (id, name, client_id, client_secret_hash, allowed_grant_types, active, created_at, created_by)
VALUES ('307aaae3-6804-4d95-beb5-4b8e1b05fe58', 'budget-service', 'budget-service',
        '$2b$12$eAoyedmTpXGJQMN./kImAeURLxApwGVcWdmeKnd04h7WKIeK39pdK', '["CLIENT_SECRET"]', true,
        current_timestamp, 'API');
-- rollback DELETE FROM workbox.api_clients WHERE client_id = 'budget-service';
