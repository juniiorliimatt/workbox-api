-- liquibase formatted sql

-- changeset oojuniin:modules-v1-seed-moto context:data labels:api,modules
-- comment: Módulo Moto (moto-service). Catálogo fechado: módulo novo entra por changeset junto com a role que libera o acesso.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM workbox.modules WHERE code = 'MOTO'
INSERT INTO workbox.modules (code, name)
VALUES ('MOTO', 'Moto');
-- rollback DELETE FROM workbox.modules WHERE code = 'MOTO';

-- changeset oojuniin:roles-v3-seed-moto context:data labels:api,roles,modules
-- comment: Role que libera o módulo Moto. Não é atribuída a ninguém aqui: só um ADMIN concede (ADMIN já recebe todos os módulos).
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM workbox.roles WHERE authority = 'MOTO' AND deleted_at IS NULL
INSERT INTO workbox.roles (authority, module_id, created_at, created_by, updated_at, updated_by)
SELECT m.code, m.id, current_timestamp, 'API', current_timestamp, 'API'
FROM workbox.modules m
WHERE m.code = 'MOTO';
-- rollback DELETE FROM workbox.roles WHERE authority = 'MOTO';
