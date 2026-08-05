-- Replaces the Veg/Non-Veg + category taxonomy with a single menu_type axis: DAILY items
-- keep rotating day to day via daily_menu_items; FIXED items (drinks, snacks, etc.) are
-- orderable every day once active and available, with no per-date scheduling.
alter table menu_items add column menu_type varchar(20);
alter table menu_items add column available boolean not null default true;

-- Existing items default to Fixed Menu, as specified. This only reclassifies them for
-- catalog/display purposes and for the new always-orderable FIXED code path added
-- alongside this migration; it does not touch orders, order_items, or daily_menu_items,
-- so order history and any existing day-by-day scheduling are fully preserved.
update menu_items set menu_type = 'FIXED' where menu_type is null;

alter table menu_items alter column menu_type set not null;

alter table menu_items drop column food_type;
alter table menu_items drop column category;
