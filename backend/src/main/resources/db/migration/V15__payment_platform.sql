-- Provider-independent payment platform: one canonical ledger (payments) for every
-- payment attempt (wallet recharge AND order checkout), refunds that can be split between
-- wallet and gateway, and admin-configurable platform fee settings.
--
-- Deliberately NOT a migration of wallet_topups: that table is left exactly as-is, frozen
-- as historical data, so this ships with zero risk to existing rows. New code stops
-- writing to it and writes to `payments` instead.

create table payments (
    id                 uuid           primary key,
    user_id            uuid           not null,
    use_case           varchar(20)    not null,
    reference_type     varchar(30),
    reference_id       varchar(64),
    subtotal           numeric(12, 2) not null,
    platform_fee       numeric(12, 2) not null default 0,
    discount           numeric(12, 2) not null default 0,
    tax                numeric(12, 2) not null default 0,
    wallet_used        numeric(12, 2) not null default 0,
    gateway_amount     numeric(12, 2) not null default 0,
    grand_total        numeric(12, 2) not null,
    currency           varchar(3)     not null default 'INR',
    provider           varchar(20)    not null,
    provider_order_id  varchar(80)    not null,
    provider_payment_id varchar(80),
    signature          varchar(255),
    status             varchar(20)    not null,
    idempotency_key    varchar(80),
    metadata           jsonb,
    created_at         timestamptz    not null,
    updated_at         timestamptz    not null,
    constraint uq_payments_provider_order unique (provider_order_id),
    constraint fk_payments_user foreign key (user_id) references users (id),
    constraint chk_payments_subtotal_non_negative check (subtotal >= 0),
    constraint chk_payments_fee_non_negative check (platform_fee >= 0),
    constraint chk_payments_grand_total_non_negative check (grand_total >= 0)
);
create index idx_payments_user on payments (user_id, created_at);
create index idx_payments_use_case on payments (use_case, status);
create index idx_payments_reference on payments (reference_type, reference_id);

-- One payment intent per (user, idempotency key), same shape as uq_orders_idem: a retried
-- "create payment" call can't spawn a second gateway order. Partial because most payments
-- (webhook-originated, admin-adjustment) won't carry a client idempotency key at all.
create unique index uq_payments_idem
    on payments (user_id, idempotency_key)
    where idempotency_key is not null;

create table refunds (
    id                 uuid           primary key,
    payment_id         uuid           not null,
    wallet_amount      numeric(12, 2) not null default 0,
    gateway_amount     numeric(12, 2) not null default 0,
    total_amount       numeric(12, 2) not null,
    reason             varchar(255),
    status             varchar(20)    not null,
    provider_refund_id varchar(80),
    created_at         timestamptz    not null,
    updated_at         timestamptz    not null,
    constraint fk_refunds_payment foreign key (payment_id) references payments (id),
    constraint chk_refunds_total_positive check (total_amount > 0)
);
create index idx_refunds_payment on refunds (payment_id);

-- Admin-configurable platform fee, one row per use case. Business revenue on top of the
-- order/recharge amount; deliberately starts disabled (enabled=false, fee_value=0) so
-- turning this feature on is a conscious admin action, not a silent price change.
create table platform_fee_settings (
    id         uuid           primary key,
    use_case   varchar(20)    not null,
    enabled    boolean        not null default false,
    fee_type   varchar(20)    not null default 'PERCENTAGE',
    fee_value  numeric(8, 4)  not null default 0,
    min_fee    numeric(12, 2),
    max_fee    numeric(12, 2),
    created_at timestamptz    not null,
    updated_at timestamptz    not null,
    constraint uq_platform_fee_use_case unique (use_case),
    constraint chk_platform_fee_value_non_negative check (fee_value >= 0)
);

insert into platform_fee_settings (id, use_case, enabled, fee_type, fee_value, created_at, updated_at)
values (gen_random_uuid(), 'WALLET_RECHARGE', false, 'PERCENTAGE', 0, now(), now()),
       (gen_random_uuid(), 'CHECKOUT',        false, 'PERCENTAGE', 0, now(), now());

-- Links an order to the payment that settled it when the new gateway/split-payment path
-- was used. Null for the existing wallet-only fast path, which is untouched.
alter table orders add column payment_id uuid;
alter table orders add constraint fk_orders_payment foreign key (payment_id) references payments (id);
