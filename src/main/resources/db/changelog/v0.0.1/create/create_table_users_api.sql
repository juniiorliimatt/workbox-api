-- liquibase formatted sql

-- changeset junior.lima:create_table_users_api-00 context:structure
-- comment create table users_api
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT count(*) FROM pg_catalog.pg_tables pt WHERE UPPER(pt.TABLENAME) = UPPER('users_api');
-- Author: Junior Lima
-- Date: 2024-11-23
-- Description: Cria a tabela users_api
create table api.users_api
(
    id                         uuid         not null,
    username                   varchar(50)  not null unique,
    password                   varchar(255) not null,
    is_enabled                 boolean      not null,
    is_account_non_expired     boolean,
    is_account_non_locked      boolean,
    is_credentials_non_expired boolean,
    created_at                 timestamp(6),
    created_by                 varchar(255),
    updated_at                 timestamp(6),
    updated_by                 varchar(255),

    primary key (id)
)