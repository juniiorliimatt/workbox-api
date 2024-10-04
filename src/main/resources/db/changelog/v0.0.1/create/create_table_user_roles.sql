create table user_roles (
    user_id uuid not null,
    role_id bigint not null,

    primary key (role_id, user_id)
)