-- liquibase formatted sql

-- changeset oojuniin:api_clients-v2-update-budget-service-secret context:data labels:api,api_clients
-- comment: Troca o secret do cliente budget-service (client_secret_hash) - novo valor definido pelo desenvolvedor, substitui o secret de exemplo do changeset de seed original. Nunca edite o changeset original ja aplicado, sempre um novo (ver README do workbox-api, secao "Clientes de introspeccao").
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM workbox.api_clients WHERE client_id = 'budget-service' AND client_secret_hash <> '$2b$12$i98RAmAWiCrMdDhQtob0DuetRbY0OXcw2omanL0vgoIJYvCPRUR4.'
UPDATE workbox.api_clients
SET client_secret_hash = '$2b$12$i98RAmAWiCrMdDhQtob0DuetRbY0OXcw2omanL0vgoIJYvCPRUR4.',
    updated_at = current_timestamp,
    updated_by = 'API'
WHERE client_id = 'budget-service';
