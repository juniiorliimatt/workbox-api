-- liquibase formatted sql

-- changeset oojuniin:users_api-v3-add-social-name context:structure labels:api,users_api
-- comment: Nome social escolhido pelo usuário ("como quer ser chamado", exibido no front) — substitui username como campo livre; o login passa a usar email.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'workbox' AND table_name = 'users_api' AND column_name = 'social_name'
ALTER TABLE workbox.users_api ADD COLUMN social_name VARCHAR(120);

-- changeset oojuniin:users_api-v3-backfill-social-name context:data labels:api,users_api
-- comment: Backfill do nome social pras linhas existentes — seed admin/user ganham nome amigável, qualquer outra linha préexistente reaproveita o username antigo como nome social.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM workbox.users_api WHERE social_name IS NOT NULL
UPDATE workbox.users_api SET social_name = 'Administrador' WHERE id = 'c269e6c8-ed9a-4204-9e84-96414776b21a';
UPDATE workbox.users_api SET social_name = 'Usuário' WHERE id = 'd310d667-3eb9-46cc-84fc-e538ec65e8b3';
UPDATE workbox.users_api SET social_name = username WHERE social_name IS NULL;

-- changeset oojuniin:users_api-v3-social-name-not-null context:structure labels:api,users_api
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'workbox' AND table_name = 'users_api' AND column_name = 'social_name' AND is_nullable = 'YES'
ALTER TABLE workbox.users_api ALTER COLUMN social_name SET NOT NULL;

-- changeset oojuniin:users_api-v3-backfill-email context:data labels:api,users_api
-- comment: Seed admin/user nunca tiveram email (eram só de bootstrap) — precisam de um antes do NOT NULL abaixo, já que login passa a ser por email.
UPDATE workbox.users_api SET email = 'admin@workbox.local' WHERE id = 'c269e6c8-ed9a-4204-9e84-96414776b21a';
UPDATE workbox.users_api SET email = 'user@workbox.local' WHERE id = 'd310d667-3eb9-46cc-84fc-e538ec65e8b3';
UPDATE workbox.users_api SET email = username || '@legacy.workbox.local' WHERE email IS NULL;

-- changeset oojuniin:users_api-v3-email-not-null context:structure labels:api,users_api
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'workbox' AND table_name = 'users_api' AND column_name = 'email' AND is_nullable = 'YES'
ALTER TABLE workbox.users_api ALTER COLUMN email SET NOT NULL;

-- changeset oojuniin:users_api-v3-drop-username-index context:structure labels:api,users_api
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'workbox' AND indexname = 'idx_users_api_username_active'
DROP INDEX workbox.idx_users_api_username_active;

-- changeset oojuniin:users_api-v3-drop-username context:structure labels:api,users_api
-- comment: Login deixa de ser por username — email (já único/obrigatório acima) é o identificador de autenticação a partir daqui.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'workbox' AND table_name = 'users_api' AND column_name = 'username'
ALTER TABLE workbox.users_api DROP COLUMN username;

-- changeset oojuniin:users_api_aud-v3-replace-username-with-social-name context:structure labels:api,users_api,audit
-- comment: Espelha em users_api_aud a troca de username por social_name na tabela principal (username sai do histórico, social_name entra — sem backfill, aud não exige NOT NULL).
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'workbox' AND table_name = 'users_api_aud' AND column_name = 'username'
ALTER TABLE workbox.users_api_aud DROP COLUMN username;
ALTER TABLE workbox.users_api_aud ADD COLUMN social_name VARCHAR(120);

-- changeset oojuniin:login_audit-v2-rename-username-to-email context:structure labels:api,login_audit
-- comment: login_audit.username sempre guardou o identificador apresentado no login — agora que esse identificador é o email, renomeia a coluna (e o índice) pra refletir isso.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'workbox' AND table_name = 'login_audit' AND column_name = 'username'
ALTER TABLE workbox.login_audit RENAME COLUMN username TO email;
ALTER INDEX workbox.idx_login_audit_username_created_at RENAME TO idx_login_audit_email_created_at;
