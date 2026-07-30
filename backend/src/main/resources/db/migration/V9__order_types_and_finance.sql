-- Teachers can collect their own food instead of having it delivered, so an order needs a
-- type and, for takeaway, a code they can show at the counter.
alter table orders add column order_type varchar(20) not null default 'DELIVERY';
alter table orders add column pickup_code varchar(12);

-- Codes only need to be unambiguous on the day they are used, which keeps them short
-- enough to read aloud. Partial index: delivery orders have no code and must not collide.
create unique index uq_order_pickup_code
    on orders (menu_date, pickup_code)
    where pickup_code is not null;

-- Profit needs a buy price as well as a sell price. Nullable because existing items have
-- none yet; reports treat a missing cost as zero, which understates COGS until it is set.
alter table menu_items add column cost_price numeric(8, 2);

-- Overheads (gas, wages, rent, supplies). Without these, "profit" would only ever be gross
-- margin on food, not what the canteen actually keeps.
create table expenses (
    id           uuid           primary key,
    expense_date date           not null,
    category     varchar(40)    not null,
    description  varchar(255),
    amount       numeric(12, 2) not null,
    created_at   timestamptz    not null,
    updated_at   timestamptz    not null,
    constraint chk_expense_amount_positive check (amount > 0)
);

create index idx_expense_date on expenses (expense_date);
