-- TuckZone demo data.
--
--   psql "<connection string>" -f backend/demo-seed.sql
--
-- Safe to run more than once: items are matched by name, menu_type is re-asserted on every
-- run, and the daily menu uses the (menu_date, menu_item_id) unique constraint.
--
-- Written against the V11 schema (menu_type + available). The older Veg/Non-Veg + category
-- columns this file used to populate were dropped by V11, which made the previous version
-- fail outright with "column food_type does not exist".
--
-- Delivery slots are NOT created here — V4 inserts them, V12 collapses them to a single
-- slot, and V13 renames it to "Recess". Ordering windows are not created either: no row
-- means OPEN, so the canteen
-- accepts orders by default until an admin closes the slot.

-- ── Catalogue ────────────────────────────────────────────────────────────────
-- DAILY items rotate and must be published per date in daily_menu_items.
-- FIXED items are orderable every day with no per-date scheduling.
-- cost_price is set so the admin dashboard's profit/loss figures are non-zero.
insert into menu_items (id, name, description, price, cost_price, menu_type,
                        available, active, created_at, updated_at)
select gen_random_uuid(), v.name, v.description, v.price, v.cost_price, v.menu_type,
       true, true, now(), now()
from (values
    ('Masala Dosa',       'Crisp dosa with potato masala and chutney',         55.00, 32.00, 'DAILY'),
    ('Rajma Chawal',      'Home-style rajma with steamed rice',                60.00, 35.00, 'DAILY'),
    ('Chole Bhature',     'Two bhature with chole and pickle',                 65.00, 38.00, 'DAILY'),
    ('Egg Fried Rice',    'Wok-tossed rice with egg and spring onion',         60.00, 34.00, 'DAILY'),
    ('Paneer Butter Masala with Roti', 'Rich paneer curry with two rotis',      75.00, 44.00, 'DAILY'),
    ('Veg Sandwich',      'Grilled sandwich with paneer and fresh vegetables', 40.00, 24.00, 'FIXED'),
    ('Paneer Roll',       'Paneer tikka wrapped in a soft roti',               50.00, 29.00, 'FIXED'),
    ('Chicken Roll',      'Spiced chicken wrapped in a soft roti',             70.00, 44.00, 'FIXED'),
    ('Samosa (2 pcs)',    'Classic potato samosas with chutney',               25.00, 12.00, 'FIXED'),
    ('Fresh Lime Soda',   'Chilled lime soda, sweet or salted',                30.00, 13.00, 'FIXED'),
    ('Cold Coffee',       'Thick cold coffee, lightly sweetened',              45.00, 22.00, 'FIXED'),
    ('Buttermilk',        'Spiced chaas, served chilled',                      25.00, 10.00, 'FIXED'),
    ('Lunch Combo',       'Dal, rice, two rotis and a seasonal sabzi',         85.00, 50.00, 'FIXED'),
    ('Snack Combo',       'Samosa, cold coffee and a sweet',                   80.00, 45.00, 'FIXED')
) as v(name, description, price, cost_price, menu_type)
where not exists (select 1 from menu_items m where m.name = v.name);

-- V11 reclassified every pre-existing item to FIXED in one sweep, so anything seeded before
-- that migration is now on the wrong side of the Daily/Fixed split. Re-assert the intended
-- type by name, which also makes a re-run self-correcting.
update menu_items m
set menu_type = v.menu_type,
    cost_price = coalesce(m.cost_price, v.cost_price),
    available = true,
    active = true,
    updated_at = now()
from (values
    ('Masala Dosa', 'DAILY', 32.00), ('Rajma Chawal', 'DAILY', 35.00),
    ('Chole Bhature', 'DAILY', 38.00), ('Egg Fried Rice', 'DAILY', 34.00),
    ('Paneer Butter Masala with Roti', 'DAILY', 44.00),
    ('Veg Sandwich', 'FIXED', 24.00), ('Paneer Roll', 'FIXED', 29.00),
    ('Chicken Roll', 'FIXED', 44.00), ('Samosa (2 pcs)', 'FIXED', 12.00),
    ('Fresh Lime Soda', 'FIXED', 13.00), ('Cold Coffee', 'FIXED', 22.00),
    ('Buttermilk', 'FIXED', 10.00), ('Lunch Combo', 'FIXED', 50.00),
    ('Snack Combo', 'FIXED', 45.00)
) as v(name, menu_type, cost_price)
where m.name = v.name and m.menu_type is distinct from v.menu_type;

-- ── Publish the DAILY items for today and tomorrow ───────────────────────────
-- Both days, so the demo works whether you show today's menu or the advance-ordering flow.
-- FIXED items are deliberately absent here: they are orderable every day without a row.
insert into daily_menu_items (id, menu_date, menu_item_id, total_quantity,
                              remaining_quantity, available, created_at, updated_at)
select gen_random_uuid(), d.menu_date, m.id, 25, 25, true, now(), now()
from menu_items m
cross join (
    select current_date as menu_date
    union all
    select current_date + 1
) d
where m.active and m.menu_type = 'DAILY'
on conflict (menu_date, menu_item_id) do nothing;

-- ── What you should see ──────────────────────────────────────────────────────
select (select count(*) from menu_items where menu_type = 'DAILY')                as daily_catalogue,
       (select count(*) from menu_items where menu_type = 'FIXED')                as fixed_catalogue,
       (select count(*) from daily_menu_items where menu_date = current_date)     as published_today,
       (select count(*) from daily_menu_items where menu_date = current_date + 1) as published_tomorrow,
       (select count(*) from delivery_slots where active)                         as active_slots;
