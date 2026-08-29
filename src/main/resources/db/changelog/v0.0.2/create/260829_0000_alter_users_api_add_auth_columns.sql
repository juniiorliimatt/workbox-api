-- liquibase formatted sql

-- changeset oojuniin:users_api-v2-add-deleted-at context:structure labels:api,users_api
-- comment: Exclusão lógica — NULL significa ativo. Precede as mudanças de unicidade abaixo (elas dependem dessa coluna existir).
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'workbox' AND table_name = 'users_api' AND column_name = 'deleted_at'
ALTER TABLE workbox.users_api ADD COLUMN deleted_at TIMESTAMP(6);

-- changeset oojuniin:users_api-v2-username-unique-only-active context:structure labels:api,users_api
-- comment: Com exclusão lógica, UNIQUE bruto em username impede reusar o username de um usuário deletado. Troca a constraint original (criada em 250630_0001) por índice único parcial só sobre linhas ativas.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = 'workbox' AND table_name = 'users_api' AND constraint_name = 'users_api_username_key'
ALTER TABLE workbox.users_api DROP CONSTRAINT users_api_username_key;
CREATE UNIQUE INDEX idx_users_api_username_active ON workbox.users_api (username) WHERE deleted_at IS NULL;

-- changeset oojuniin:users_api-v2-add-email context:structure labels:api,users_api
-- comment: Email (recuperação de senha, login social) — nullable pra não quebrar linhas seed existentes. Único só entre linhas ativas, mesma razão do username acima — sem UNIQUE inline na coluna.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'workbox' AND table_name = 'users_api' AND column_name = 'email'
ALTER TABLE workbox.users_api ADD COLUMN email VARCHAR(255);
CREATE UNIQUE INDEX idx_users_api_email_active ON workbox.users_api (email) WHERE deleted_at IS NULL AND email IS NOT NULL;

-- changeset oojuniin:users_api-v2-add-token-version context:structure labels:api,users_api
-- comment: Versão do token JWT do usuário — incrementada em logout/troca de senha/reset pra revogar tokens já emitidos.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'workbox' AND table_name = 'users_api' AND column_name = 'token_version'
ALTER TABLE workbox.users_api ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0;

-- changeset oojuniin:users_api-v2-add-failed-login-attempts context:structure labels:api,users_api
-- comment: Contador de tentativas de login falhas consecutivas — zerado no sucesso, aciona lockout automático.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'workbox' AND table_name = 'users_api' AND column_name = 'failed_login_attempts'
ALTER TABLE workbox.users_api ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0;

-- changeset oojuniin:users_api-v2-add-locked-until context:structure labels:api,users_api
-- comment: Timestamp de expiração do lockout automático por brute-force — NULL significa não bloqueado. Desbloqueio é por tempo, não exige ação de admin.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'workbox' AND table_name = 'users_api' AND column_name = 'locked_until'
ALTER TABLE workbox.users_api ADD COLUMN locked_until TIMESTAMP(6);

-- changeset oojuniin:roles-v2-add-deleted-at context:structure labels:api,roles
-- comment: Exclusão lógica pro RoleController novo, mesmo padrão de users_api.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'workbox' AND table_name = 'roles' AND column_name = 'deleted_at'
ALTER TABLE workbox.roles ADD COLUMN deleted_at TIMESTAMP(6);

-- changeset oojuniin:roles-v2-authority-unique-only-active context:structure labels:api,roles
-- comment: authority nunca teve constraint de unicidade — corrige aqui já como índice parcial (só linhas ativas), consistente com o padrão de exclusão lógica.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'workbox' AND indexname = 'idx_roles_authority_active'
CREATE UNIQUE INDEX idx_roles_authority_active ON workbox.roles (authority) WHERE deleted_at IS NULL;
