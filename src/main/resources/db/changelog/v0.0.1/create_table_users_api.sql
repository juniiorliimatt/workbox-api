create table users_api
(
    id               uuid         not null,
    enabled          boolean      not null,
    created_at       timestamp(6),
    updated_at       timestamp(6),
    created_by       varchar(255),
    last_modified_by varchar(255),
    password         varchar(255) not null,
    username         varchar(255) not null unique,
    primary key (id)
)