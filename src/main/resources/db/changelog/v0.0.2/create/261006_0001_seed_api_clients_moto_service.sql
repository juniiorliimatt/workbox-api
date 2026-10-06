-- liquibase formatted sql

-- changeset oojuniin:api_clients-v5-seed-moto-service context:data labels:api,api_clients
-- comment: Cliente inicial do moto-service, secret de estudo local (ver .env.example na raiz do monorepo). Em producao, gere um secret forte e insira o cliente real via changeset novo (nunca edite este arquivo ja aplicado) - ver README do workbox-api, secao "Clientes de introspeccao".
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM workbox.api_clients WHERE client_id = 'moto-service'
INSERT INTO workbox.api_clients (id, name, client_id, client_secret_hash, allowed_grant_types, active, created_at, created_by)
VALUES ('ae895766-d9a6-427b-9818-1ad56a370ffa', 'moto-service', 'moto-service',
        '$2b$12$Rq0qODtm8I.ZwQU1rcqdWOHWI5NFY.zq/baqjnYNJcS31qV76mNBO', '["CLIENT_SECRET"]', true,
        current_timestamp, 'API');
-- rollback DELETE FROM workbox.api_clients WHERE client_id = 'moto-service';
