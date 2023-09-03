
insert into users(username, password, enabled)
values ('jlong', 'password', true)
on conflict on constraint users_pkey do nothing;

insert into users(username, password, enabled)
values ('rwinch', 'p@ssw0rd', true)
on conflict on constraint users_pkey do nothing;

insert into authorities (username, authority)
values ('jlong', 'user')
on conflict on constraint username_authority do nothing;

insert into authorities (username, authority)
values ('rwinch', 'user')
on conflict on constraint username_authority do nothing;

insert into authorities (username, authority)
values ('rwinch', 'admin')
on conflict on constraint username_authority do nothing;

