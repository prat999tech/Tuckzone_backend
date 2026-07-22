-- Human-friendly, monotonically increasing order numbers (separate from the UUID id).
create sequence order_number_seq start with 1001 increment by 1;

-- Delivery slots = recurring daily windows: order before the cutoff, receive at delivery
-- time. Seeded with two typical recesses.
create table delivery_slots (
    id                uuid        primary key,
    name              varchar(60) not null,
    order_cutoff_time time        not null,
    delivery_time     time        not null,
    active            boolean     not null default true,
    created_at        timestamptz not null,
    updated_at        timestamptz not null
);

insert into delivery_slots (id, name, order_cutoff_time, delivery_time, active, created_at, updated_at)
values (gen_random_uuid(), 'Morning Recess', '09:30:00', '10:45:00', true, now(), now()),
       (gen_random_uuid(), 'Lunch Recess',   '11:30:00', '12:45:00', true, now(), now());

create table orders (
    id                             uuid          primary key,
    order_number                   bigint        not null,
    placed_by_user_id              uuid          not null,
    beneficiary_student_profile_id uuid,
    recipient_name                 varchar(120)  not null,
    delivery_location              varchar(200)  not null,
    slot_id                        uuid          not null,
    menu_date                      date          not null,
    status                         varchar(30)   not null,
    payment_method                 varchar(20)   not null,
    payment_status                 varchar(20)   not null,
    total_amount                   numeric(12, 2) not null,
    delivery_person_name           varchar(120),
    idempotency_key                varchar(80),
    created_at                     timestamptz   not null,
    updated_at                     timestamptz   not null,
    constraint uq_orders_number  unique (order_number),
    -- One order per (user, idempotency key): a retried placement can't create a second order.
    constraint uq_orders_idem    unique (placed_by_user_id, idempotency_key),
    constraint fk_orders_user    foreign key (placed_by_user_id) references users (id),
    constraint fk_orders_student foreign key (beneficiary_student_profile_id) references student_profiles (id),
    constraint fk_orders_slot    foreign key (slot_id) references delivery_slots (id),
    constraint chk_orders_total_non_negative check (total_amount >= 0)
);
create index idx_orders_date_status on orders (menu_date, status);
create index idx_orders_user on orders (placed_by_user_id, created_at);

-- Line items snapshot the name and price at order time, so later catalog changes never
-- rewrite a customer's history.
create table order_items (
    id           uuid          primary key,
    order_id     uuid          not null,
    menu_item_id uuid          not null,
    item_name    varchar(120)  not null,
    unit_price   numeric(8, 2) not null,
    quantity     integer       not null,
    line_total   numeric(12, 2) not null,
    created_at   timestamptz   not null,
    updated_at   timestamptz   not null,
    constraint fk_oi_order     foreign key (order_id) references orders (id) on delete cascade,
    constraint fk_oi_menu_item foreign key (menu_item_id) references menu_items (id),
    constraint chk_oi_qty_positive check (quantity > 0)
);
create index idx_oi_order on order_items (order_id);
create index idx_oi_menu_item on order_items (menu_item_id);
