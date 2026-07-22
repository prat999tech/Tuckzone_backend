-- One wallet per user. balance is a materialized running total; the CHECK is a hard
-- backstop so it can never go negative even if application logic has a bug.
create table wallets (
    id         uuid          primary key,
    user_id    uuid          not null,
    balance    numeric(12, 2) not null default 0,
    created_at timestamptz   not null,
    updated_at timestamptz   not null,
    constraint uq_wallet_user unique (user_id),
    constraint fk_wallet_user foreign key (user_id) references users (id),
    constraint chk_wallet_balance_non_negative check (balance >= 0)
);

-- Append-only ledger. Every balance change writes exactly one row here. amount is always
-- positive; the type column carries the direction. balance_after snapshots the running
-- total for a clean statement.
create table wallet_transactions (
    id             uuid          primary key,
    wallet_id      uuid          not null,
    type           varchar(20)   not null,
    amount         numeric(12, 2) not null,
    balance_after  numeric(12, 2) not null,
    reference_type varchar(30),
    reference_id   varchar(64),
    description    varchar(255),
    created_at     timestamptz   not null,
    updated_at     timestamptz   not null,
    constraint fk_wtx_wallet foreign key (wallet_id) references wallets (id),
    constraint chk_wtx_amount_positive check (amount > 0)
);
create index idx_wtx_wallet on wallet_transactions (wallet_id, created_at);

-- A top-up attempt. gateway_order_id is unique, which is the key that makes crediting
-- idempotent against retried payment callbacks.
create table wallet_topups (
    id                 uuid          primary key,
    wallet_id          uuid          not null,
    amount             numeric(12, 2) not null,
    gateway_order_id   varchar(80)   not null,
    gateway_payment_id varchar(80),
    status             varchar(20)   not null,
    created_at         timestamptz   not null,
    updated_at         timestamptz   not null,
    constraint uq_topup_order   unique (gateway_order_id),
    constraint fk_topup_wallet  foreign key (wallet_id) references wallets (id),
    constraint chk_topup_amount_positive check (amount > 0)
);

-- Parent <-> child links. Unique so the same parent can't link the same child twice.
create table parent_child_links (
    id                 uuid        primary key,
    parent_user_id     uuid        not null,
    student_profile_id uuid        not null,
    created_at         timestamptz not null,
    updated_at         timestamptz not null,
    constraint uq_parent_child  unique (parent_user_id, student_profile_id),
    constraint fk_link_parent   foreign key (parent_user_id) references users (id),
    constraint fk_link_student  foreign key (student_profile_id) references student_profiles (id)
);
create index idx_link_parent on parent_child_links (parent_user_id);
