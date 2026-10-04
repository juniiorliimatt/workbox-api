-- liquibase formatted sql

-- changeset oojuniin:roles-v3-add-module-id context:structure labels:api,roles,modules
-- comment: Vinculo role -> modulo (nullable: ADMIN e USER nao liberam modulo; uma role aponta no maximo para um modulo, um modulo pode ter varias roles).
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'workbox' AND table_name = 'roles' AND column_name = 'module_id'
ALTER TABLE workbox.roles ADD COLUMN module_id BIGINT;
ALTER TABLE workbox.roles ADD CONSTRAINT fk_roles_module FOREIGN KEY (module_id) REFERENCES workbox.modules (id);
CREATE INDEX idx_roles_module_id ON workbox.roles (module_id);

-- changeset oojuniin:roles-aud-v3-add-module-id context:structure labels:api,roles,audit
-- comment: Espelha roles.module_id em roles_aud (Envers audita o vinculo, mas nao o catalogo de modulos - targetAuditMode NOT_AUDITED, sem FK).
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'workbox' AND table_name = 'roles_aud' AND column_name = 'module_id'
ALTER TABLE workbox.roles_aud ADD COLUMN module_id BIGINT;
