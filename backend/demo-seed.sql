-- TuckZone demo data.
--
--   psql "<connection string>" -f backend/demo-seed.sql
--
-- Safe to run more than once: items are matched by name and the daily menu uses the
-- (menu_date, menu_item_id) unique constraint, so a second run changes nothing.
--
-- Delivery slots are NOT created here — migration V4 already inserts "Morning Recess"
-- and "Lunch Recess". Ordering windows are not created either: no row means OPEN, so the
-- canteen accepts orders by default until an admin closes a slot.

-- ── Catalogue ────────────────────────────────────────────────────────────────
-- cost_price is set so the admin dashboard's profit/loss figures are non-zero and the
-- reports screen has something real to show.
insert into menu_items (id, name, description, price, cost_price, food_type, category,
                        active, created_at, updated_at)
select gen_random_uuid(), v.name, v.description, v.price, v.cost_price, v.food_type,
       v.category, true, now(), now()
from (values
    ('Veg Sandwich',      'Grilled sandwich with paneer and fresh vegetables', 40.00, 24.00, 'VEG',     'SNACKS'),
    ('Masala Dosa',       'Crisp dosa with potato masala and chutney',         55.00, 32.00, 'VEG',     'MEALS'),
    ('Rajma Chawal',      'Home-style rajma with steamed rice',                60.00, 35.00, 'VEG',     'MEALS'),
    ('Chole Bhature',     'Two bhature with chole and pickle',                 65.00, 38.00, 'VEG',     'MEALS'),
    ('Paneer Roll',       'Paneer tikka wrapped in a soft roti',               50.00, 29.00, 'VEG',     'SNACKS'),
    ('Chicken Roll',      'Spiced chicken wrapped in a soft roti',             70.00, 44.00, 'NON_VEG', 'SNACKS'),
    ('Egg Fried Rice',    'Wok-tossed rice with egg and spring onion',         60.00, 34.00, 'NON_VEG', 'MEALS'),
    ('Samosa (2 pcs)',    'Classic potato samosas with chutney',               25.00, 12.00, 'VEG',     'SNACKS'),
    ('Fresh Lime Soda',   'Chilled lime soda, sweet or salted',                30.00, 13.00, 'VEG',     'DRINKS'),
    ('Cold Coffee',       'Thick cold coffee, lightly sweetened',              45.00, 22.00, 'VEG',     'DRINKS'),
    ('Buttermilk',        'Spiced chaas, served chilled',                      25.00, 10.00, 'VEG',     'DRINKS'),
    ('Lunch Combo',       'Dal, rice, two rotis and a seasonal sabzi',         85.00, 50.00, 'VEG',     'COMBOS'),
    ('Snack Combo',       'Samosa, cold coffee and a sweet',                   80.00, 45.00, 'VEG',     'COMBOS')
) as v(name, description, price, cost_price, food_type, category)
where not exists (select 1 from menu_items m where m.name = v.name);

-- ── Published menu for today and tomorrow ────────────────────────────────────
-- Both days, so the demo works whether you show today's menu or the advance-ordering
-- flow — and so "pre-order for tomorrow" has something to land on after the cutoff.
insert into daily_menu_items (id, menu_date, menu_item_id, total_quantity,
                              remaining_quantity, available, created_at, updated_at)
select gen_random_uuid(), d.menu_date, m.id, 25, 25, true, now(), now()
from menu_items m
cross join (
    select current_date as menu_date
    union all
    select current_date + 1
) d
where m.active
on conflict (menu_date, menu_item_id) do nothing;

-- ── What you should see ──────────────────────────────────────────────────────
select (select count(*) from menu_items)                                    as catalogue_items,
       (select count(*) from daily_menu_items where menu_date = current_date)     as on_menu_today,
       (select count(*) from daily_menu_items where menu_date = current_date + 1) as on_menu_tomorrow,
       (select count(*) from delivery_slots where active)                   as active_slots;
