-- Collapses the two-recess model from V4 into a single ordering slot. Existing slot rows
-- are kept (not deleted) so historical orders still resolve their slot via the FK; only
-- one stays active, and it is renamed so new orders and every UI surface show a single
-- "Recess Time" instead of asking the customer to pick a delivery time.
update delivery_slots
set active = false
where name = 'Lunch Recess';

update delivery_slots
set name = 'Recess Time'
where name = 'Morning Recess';
