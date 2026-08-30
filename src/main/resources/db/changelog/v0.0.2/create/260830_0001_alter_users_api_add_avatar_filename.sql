-- liquibase formatted sql

-- changeset oojuniin:users_api-v3-add-avatar-filename context:structure labels:api,users_api
-- comment: Nome do arquivo (gerado pelo servidor, UUID) do avatar do usuário em uploads/avatars — NULL até o primeiro upload.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'workbox' AND table_name = 'users_api' AND column_name = 'avatar_filename'
ALTER TABLE workbox.users_api ADD COLUMN avatar_filename VARCHAR(255);

-- changeset oojuniin:users_api_aud-v3-add-avatar-filename context:structure labels:api,users_api,audit
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'workbox' AND table_name = 'users_api_aud' AND column_name = 'avatar_filename'
ALTER TABLE workbox.users_api_aud ADD COLUMN avatar_filename VARCHAR(255);
