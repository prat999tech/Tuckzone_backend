-- Devices that can receive push notifications. One row per app install; a user with a
-- phone and a tablet has two. The token is unique because FCM reissues the same token to
-- whichever install currently owns it — if it moves to another account, it must move here
-- too rather than being duplicated.
create table device_tokens (
    id           uuid          primary key,
    user_id      uuid          not null,
    token        varchar(512)  not null,
    platform     varchar(20)   not null,
    last_seen_at timestamptz   not null,
    created_at   timestamptz   not null,
    updated_at   timestamptz   not null,
    constraint uq_device_token unique (token),
    constraint fk_device_user  foreign key (user_id) references users (id) on delete cascade
);
create index idx_device_user on device_tokens (user_id);

-- Transactional outbox.
--
-- A row is written in the SAME transaction as the change that caused it, so a
-- notification can never announce an order that failed to save, and a saved order can
-- never silently lose its notification. Delivery happens after commit; the scheduler only
-- picks up whatever the immediate attempt failed to send.
create table notification_outbox (
    id              uuid          primary key,
    user_id         uuid          not null,
    event_type      varchar(40)   not null,
    channel         varchar(20)   not null,
    title           varchar(150)  not null,
    body            varchar(500)  not null,
    payload         text,
    status          varchar(20)   not null,
    attempts        integer       not null default 0,
    next_attempt_at timestamptz   not null,
    sent_at         timestamptz,
    last_error      varchar(500),
    created_at      timestamptz   not null,
    updated_at      timestamptz   not null,
    constraint fk_outbox_user foreign key (user_id) references users (id) on delete cascade,
    constraint chk_outbox_attempts_non_negative check (attempts >= 0)
);

-- Drives the sweeper's "what is still owed?" query.
create index idx_outbox_claim on notification_outbox (status, next_attempt_at);
create index idx_outbox_user on notification_outbox (user_id, created_at desc);
