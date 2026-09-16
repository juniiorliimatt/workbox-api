-- liquibase formatted sql

-- changeset oojuniin:api_clients-v3-seed-notes-service context:data labels:api,api_clients
-- comment: Cliente inicial do notes-service, secret de estudo local (ver .env.example na raiz do monorepo). Em producao, gere um secret forte e insira o cliente real via changeset novo (nunca edite este arquivo ja aplicado) - ver README do workbox-api, secao "Clientes de introspeccao".
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM workbox.api_clients WHERE client_id = 'notes-service'
INSERT INTO workbox.api_clients (id, name, client_id, client_secret_hash, allowed_grant_types, active, created_at, created_by)
VALUES ('58db8798-084a-47c0-8656-32545679a5a7', 'notes-service', 'notes-service',
        '$2b$12$9InDLiPBgPZQKGwKSvSxg.bmXsjLLoaOlKq38xGHd7cYRApUG1OCG', '["CLIENT_SECRET"]', true,
        current_timestamp, 'API');
-- rollback DELETE FROM workbox.api_clients WHERE client_id = 'notes-service';
