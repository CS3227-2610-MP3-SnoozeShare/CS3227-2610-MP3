create table users (
    id uuid primary key,
    firebase_uid varchar(128) not null,
    email varchar(320) not null,
    role varchar(16) not null,
    display_name varchar(80),
    status varchar(16) not null default 'ACTIVE',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint users_firebase_uid_key unique (firebase_uid),
    constraint users_role_check check (role in ('GUEST', 'HOST', 'AGENT')),
    constraint users_status_check check (status in ('ACTIVE', 'INACTIVE'))
);

create table wallets (
    id uuid primary key,
    user_id uuid not null,
    currency varchar(3) not null default 'SGD',
    balance numeric(19, 2) not null default 0.00,
    constraint wallets_user_id_key unique (user_id),
    constraint wallets_user_id_fkey foreign key (user_id) references users (id)
);
