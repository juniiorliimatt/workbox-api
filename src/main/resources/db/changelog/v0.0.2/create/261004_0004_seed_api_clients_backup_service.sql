-- liquibase formatted sql

-- changeset oojuniin:api_clients-v5-seed-backup-service context:data labels:api,api_clients
-- comment: Cliente inicial do backup-service, secret de estudo local (ver .env.example na raiz do monorepo). Em producao, gere um secret forte e insira o cliente real via changeset novo (nunca edite este arquivo ja aplicado) - ver README do workbox-api, secao "Clientes de introspeccao".
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM workbox.api_clients WHERE client_id = 'backup-service'
INSERT INTO workbox.api_clients (id, name, client_id, client_secret_hash, allowed_grant_types, active, created_at, created_by)
VALUES ('58386d94-7a99-4756-bf96-5c0c7c39bc3a', 'backup-service', 'backup-service',
        '$2b$12$5x8Q3iDaodFeXPKLhx9wu.c4y9yxvR1H.YeHOpA4uIh0ON39Juig2', '["CLIENT_SECRET"]', true,
        current_timestamp, 'API');
-- rollback DELETE FROM workbox.api_clients WHERE client_id = 'backup-service';
