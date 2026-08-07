-- Permanently deleting a menu item must not be blocked by, or corrupt, past orders.
--
-- order_items already snapshots item_name/unit_price/quantity/line_total at order time
-- (see V4), so historical display never actually depended on menu_items for those. The
-- only things still tying an order_item to a *live* catalog row were the NOT NULL FK
-- (which blocks deleting a menu item with any order history at all) and two reporting
-- queries that read cost_price/menu_type off the live join. This migration finishes the
-- snapshot so a menu item can be deleted with zero effect on any order ever placed for it.

-- The FK becomes optional and self-clearing: deleting a menu_items row now nulls out
-- menu_item_id on every order_item that referenced it, instead of being rejected.
alter table order_items alter column menu_item_id drop not null;
alter table order_items drop constraint fk_oi_menu_item;
alter table order_items
    add constraint fk_oi_menu_item foreign key (menu_item_id) references menu_items (id) on delete set null;

-- Same snapshot treatment as item_name/unit_price: taken once at order time, read forever
-- after regardless of what happens to the catalog row.
alter table order_items add column cost_price numeric(8, 2);
alter table order_items add column menu_type varchar(20);

-- Backfill every order placed before this migration, so their historical reports are
-- accurate too — not just orders placed from here on.
update order_items oi
set cost_price = mi.cost_price,
    menu_type = mi.menu_type
from menu_items mi
where oi.menu_item_id = mi.id
  and oi.cost_price is null;
