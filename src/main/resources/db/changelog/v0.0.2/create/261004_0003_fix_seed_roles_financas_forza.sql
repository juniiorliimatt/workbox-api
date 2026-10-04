-- liquibase formatted sql

-- changeset oojuniin:roles-v3-seed-financas-if-absent context:data labels:api,roles,modules
-- comment: Corrige o seed 261004_0002, cujo precondition era "todas ou nenhuma": num banco que ja tinha a role FORZA (criada a mao) ele foi ignorado e a role FINANCAS nao nasceu. Aqui cada role e tratada sozinha. Nao concede acesso a ninguem (so um ADMIN atribui a role ao usuario).
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM workbox.roles WHERE authority = 'FINANCAS' AND deleted_at IS NULL
INSERT INTO workbox.roles (authority, module_id, created_at, created_by, updated_at, updated_by)
SELECT m.code, m.id, current_timestamp, 'API', current_timestamp, 'API'
FROM workbox.modules m
WHERE m.code = 'FINANCAS';
-- rollback DELETE FROM workbox.roles WHERE authority = 'FINANCAS';

-- changeset oojuniin:roles-v3-link-existing-forza-role context:data labels:api,roles,modules
-- comment: Se a role FORZA ja existia sem modulo (criada antes do catalogo de modulos), liga ao modulo FORZA. So quem ja recebeu essa role de um ADMIN passa a acessar o modulo.
UPDATE workbox.roles
SET module_id = (SELECT id FROM workbox.modules WHERE code = 'FORZA'),
    updated_at = current_timestamp,
    updated_by = 'API'
WHERE authority = 'FORZA' AND deleted_at IS NULL AND module_id IS NULL;
-- rollback UPDATE workbox.roles SET module_id = NULL WHERE authority = 'FORZA';
