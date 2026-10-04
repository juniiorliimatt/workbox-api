-- liquibase formatted sql

-- changeset oojuniin:roles-v3-seed-financas-forza context:data labels:api,roles,modules
-- comment: Roles que liberam os modulos Financas e Forza. Nao sao atribuidas a ninguem aqui: quem ja tem USER fica sem acesso aos modulos ate um ADMIN conceder a role (decisao de produto - USER e so a role inicial).
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM workbox.roles WHERE authority IN ('FINANCAS', 'FORZA') AND deleted_at IS NULL
INSERT INTO workbox.roles (authority, module_id, created_at, created_by, updated_at, updated_by)
SELECT m.code, m.id, current_timestamp, 'API', current_timestamp, 'API'
FROM workbox.modules m
WHERE m.code IN ('FINANCAS', 'FORZA');
-- rollback DELETE FROM workbox.roles WHERE authority IN ('FINANCAS', 'FORZA');
