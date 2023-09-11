create table if not exists rsa_key_pairs
(
    id          varchar(1000) not null primary key,
    private_key text          not null,
    public_key  text          not null,
    created     date          not null,
    unique (id, created )
);