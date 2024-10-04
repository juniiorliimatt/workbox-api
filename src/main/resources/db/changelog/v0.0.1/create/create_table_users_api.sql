create table users_api
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