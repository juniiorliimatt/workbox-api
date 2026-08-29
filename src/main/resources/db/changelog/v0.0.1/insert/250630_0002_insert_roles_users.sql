-- liquibase formatted sql

-- changeset oojuniin:insert-initial-roles-v1 context:data labels:security
-- comment: Insere as roles basicas de sistema (ADMIN, USER) apenas se elas nao existirem
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM api.roles WHERE authority IN ('ADMIN', 'USER')
-- rollback DELETE FROM api.roles WHERE authority IN ('ADMIN', 'USER');
INSERT INTO api.roles (authority, created_at, created_by, updated_at, updated_by)
VALUES ('ADMIN', current_timestamp, 'API', current_timestamp, 'API'),
       ('USER', current_timestamp, 'API', current_timestamp, 'API');
-- rollback DELETE FROM api.roles WHERE authority IN ('ADMIN', 'USER');

-- changeset oojuniin:insert-initial-users-v1 context:data labels:security,users
-- comment: Insere os usuários iniciais do sistema (admin, user) se eles ainda não existirem.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM api.users_api WHERE username IN ('admin', 'user');
INSERT INTO api.users_api (id, username, password, is_enabled, is_account_non_expired,
                           is_account_non_locked, is_credentials_non_expired, created_at,
                           created_by, updated_at, updated_by)
VALUES ('c269e6c8-ed9a-4204-9e84-96414776b21a', 'admin',
        '$2a$12$daCzz/AQ7plxV200FuyQn.4JRNdGAcxKVDQjn9gQ0wqozZxpRn6NS', true, true, true, true,
        current_timestamp, 'API', current_timestamp, 'API'),
       ('d310d667-3eb9-46cc-84fc-e538ec65e8b3', 'user',
        '$2a$12$0.AX3SuOcP1mFEUJ2cL//eQxgTHGn3NEKwDk0ZrhRtEaJsejY7Y5i', true, true, true, true,
        current_timestamp, 'API', current_timestamp, 'API');
-- rollback DELETE FROM api.users_api WHERE id IN ('c269e6c8-ed9a-4204-9e84-96414776b21a', 'd310d667-3eb9-46cc-84fc-e538ec65e8b3');


-- changeset oojuniin:link-initial-user-roles-v1 context:data labels:security,users
-- comment: Associa as roles aos usuários iniciais (admin, user) se a associação ainda não existir.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM api.user_roles WHERE user_id IN ('c269e6c8-ed9a-4204-9e84-96414776b21a', 'd310d667-3eb9-46cc-84fc-e538ec65e8b3');
INSERT INTO api.user_roles (user_id, role_id)
VALUES ('c269e6c8-ed9a-4204-9e84-96414776b21a', 1),
       ('d310d667-3eb9-46cc-84fc-e538ec65e8b3', 2);
-- rollback DELETE FROM api.user_roles WHERE user_id IN ('c269e6c8-ed9a-4204-9e84-96414776b21a', 'd310d667-3eb9-46cc-84fc-e538ec65e8b3');
