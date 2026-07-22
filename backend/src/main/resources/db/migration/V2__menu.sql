-- Permanent catalog of food items. Items are never hard-deleted (past orders reference
-- them); retiring an item sets active = false.
create table menu_items (
    id          uuid          primary key,
    name        varchar(120)  not null,
    description varchar(500),
    price       numeric(8, 2) not null,
    food_type   varchar(20)   not null,
    category    varchar(20)   not null,
    image_url   varchar(500),
    allergens   varchar(255),
    active      boolean       not null default true,
    created_at  timestamptz   not null,
    updated_at  timestamptz   not null,
    constraint chk_menu_item_price_non_negative check (price >= 0)
);

-- What is offered on a given date, with stock. remaining_quantity is what ordering
-- decrements; it can never exceed total_quantity or drop below zero.
create table daily_menu_items (
    id                 uuid        primary key,
    menu_date          date        not null,
    menu_item_id       uuid        not null,
    total_quantity     integer     not null,
    remaining_quantity integer     not null,
    available          boolean     not null default true,
    created_at         timestamptz not null,
    updated_at         timestamptz not null,
    constraint uq_daily_menu_date_item unique (menu_date, menu_item_id),
    constraint fk_daily_menu_item      foreign key (menu_item_id) references menu_items (id),
    constraint chk_total_non_negative     check (total_quantity >= 0),
    constraint chk_remaining_non_negative check (remaining_quantity >= 0),
    constraint chk_remaining_within_total check (remaining_quantity <= total_quantity)
);

create index idx_daily_menu_date on daily_menu_items (menu_date);
