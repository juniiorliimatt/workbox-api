-- liquibase formatted sql

-- changeset oojuniin:api_clients-v4-seed-forza-telemetry-service context:data labels:api,api_clients
-- comment: Cliente inicial do forza-telemetry-service, secret de estudo local (ver .env.example na raiz do monorepo). Em producao, gere um secret forte e insira o cliente real via changeset novo (nunca edite este arquivo ja aplicado) - ver README do workbox-api, secao "Clientes de introspeccao".
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM workbox.api_clients WHERE client_id = 'forza-telemetry-service'
INSERT INTO workbox.api_clients (id, name, client_id, client_secret_hash, allowed_grant_types, active, created_at, created_by)
VALUES ('ed10b25b-6963-4d54-8b61-fbd70e8a9329', 'forza-telemetry-service', 'forza-telemetry-service',
        '$2b$12$Ky1EW5abKqdUeD0gqD2lgO7MTdDCP3Z6qTbOGnFwleRTFWQAV6b3u', '["CLIENT_SECRET"]', true,
        current_timestamp, 'API');
-- rollback DELETE FROM workbox.api_clients WHERE client_id = 'forza-telemetry-service';
