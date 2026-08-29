-- liquibase formatted sql

-- changeset oojuniin:users_api-add-mfa-secret context:structure labels:api,mfa
-- comment: Segredo TOTP (Base32) do usuário — NULL até o primeiro /mfa/enroll.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'workbox' AND table_name = 'users_api' AND column_name = 'mfa_secret'
ALTER TABLE workbox.users_api ADD COLUMN mfa_secret VARCHAR(64);

-- changeset oojuniin:users_api-add-mfa-enabled context:structure labels:api,mfa
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'workbox' AND table_name = 'users_api' AND column_name = 'mfa_enabled'
ALTER TABLE workbox.users_api ADD COLUMN mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE;

-- changeset oojuniin:users_api_aud-add-mfa-enabled context:structure labels:api,mfa,audit
-- comment: mfa_secret fica de fora do histórico de auditoria (@NotAudited na entidade) — é segredo vivo, não dado de negócio a reter em duplicidade.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'workbox' AND table_name = 'users_api_aud' AND column_name = 'mfa_enabled'
ALTER TABLE workbox.users_api_aud ADD COLUMN mfa_enabled BOOLEAN;
