-- liquibase formatted sql

-- changeset junior.lima:create_table_user_roles-00 context:structure
-- comment create table user_roles
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT count(*) FROM pg_catalog.pg_tables pt WHERE UPPER(pt.TABLENAME) = UPPER('user_roles');
-- Author: Junior Lima
-- Date: 2024-11-23
-- Description: Cria a tabela user_roles
create table api.user_roles (
    user_id uuid not null,
    role_id bigint not null,

    primary key (role_id, user_id)
)