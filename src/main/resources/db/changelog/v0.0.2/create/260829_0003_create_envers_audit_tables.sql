-- liquibase formatted sql

-- changeset oojuniin:envers-v1-rev-info context:structure labels:api,audit
-- comment: Tabela de revisões do Hibernate Envers, compartilhada entre todas as entidades @Audited. Colunas id/timestamp são o mapeamento real de DefaultRevisionEntity (int id, long timestamp — sem override de nome). @GeneratedValue sem estratégia explícita resolve pra SEQUENCE no Postgres (Hibernate 6), convenção de nome "{table}_seq" — não IDENTITY. username vem do RevisionListenerImpl (SecurityContext).
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'workbox' AND table_name = 'rev_info'
-- INCREMENT BY 50 é o allocationSize default do JPA/Hibernate pra @GeneratedValue sem
-- @SequenceGenerator explícito — tem que bater exatamente com o que o Hibernate espera.
CREATE SEQUENCE workbox.rev_info_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE workbox.rev_info
(
    id        INTEGER PRIMARY KEY DEFAULT nextval('workbox.rev_info_seq'),
    timestamp BIGINT,
    username  VARCHAR(255) NOT NULL
);

-- changeset oojuniin:envers-v1-users-api-aud context:structure labels:api,audit
-- comment: Histórico de revisões de users_api (Hibernate Envers @Audited) — espelha as colunas da tabela principal, exceto a associação roles (@NotAudited). rev/revtype são os nomes default do Envers pra FK de revisão, independente do nome do @Id na entidade de revisão.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'workbox' AND table_name = 'users_api_aud'
CREATE TABLE workbox.users_api_aud
(
    id                         UUID         NOT NULL,
    rev                        INTEGER      NOT NULL REFERENCES workbox.rev_info (id),
    revtype                    SMALLINT,
    username                   VARCHAR(50),
    password                   VARCHAR(255),
    email                      VARCHAR(255),
    is_enabled                 BOOLEAN,
    is_account_non_expired     BOOLEAN,
    is_account_non_locked      BOOLEAN,
    is_credentials_non_expired BOOLEAN,
    token_version              BIGINT,
    failed_login_attempts      INT,
    locked_until               TIMESTAMP(6),
    deleted_at                 TIMESTAMP(6),
    created_at                 TIMESTAMP(6),
    updated_at                 TIMESTAMP(6),
    created_by                 VARCHAR(255),
    updated_by                 VARCHAR(255),
    PRIMARY KEY (id, rev)
);

-- changeset oojuniin:envers-v1-roles-aud context:structure labels:api,audit
-- comment: Histórico de revisões de roles (Hibernate Envers @Audited) — espelha as colunas da tabela principal, exceto a associação users (@NotAudited).
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'workbox' AND table_name = 'roles_aud'
CREATE TABLE workbox.roles_aud
(
    id         BIGINT  NOT NULL,
    rev        INTEGER NOT NULL REFERENCES workbox.rev_info (id),
    revtype    SMALLINT,
    authority  VARCHAR(255),
    created_at TIMESTAMP(6),
    created_by VARCHAR(255),
    updated_at TIMESTAMP(6),
    updated_by VARCHAR(255),
    deleted_at TIMESTAMP(6),
    PRIMARY KEY (id, rev)
);
