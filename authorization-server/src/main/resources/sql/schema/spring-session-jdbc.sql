-- sessions
create table if not exists spring_session
(
    primary_id            character(36) primary key not null,
    session_id            character(36)             not null,
    creation_time         bigint                    not null,
    last_access_time      bigint                    not null,
    max_inactive_interval integer                   not null,
    expiry_time           bigint                    not null,
    principal_name        character varying(100)
);
create unique index if not exists spring_session_ix1 on spring_session using btree (session_id);
create index if not exists spring_session_ix2 on spring_session using btree (expiry_time);
create index if not exists spring_session_ix3 on spring_session using btree (principal_name);

-- session attributes
create table if not exists spring_session_attributes
(
    session_primary_id character(36)          not null,
    attribute_name     character varying(200) not null,
    attribute_bytes    bytea                  not null,
    primary key (session_primary_id, attribute_name),
    foreign key (session_primary_id) references spring_session (primary_id)
        match simple on update no action on delete cascade
);

