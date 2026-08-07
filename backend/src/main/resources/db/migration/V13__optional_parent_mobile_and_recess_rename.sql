-- Parent's mobile is now optional at student registration: a student can be linked to a
-- parent later once one is recorded, but must not be blocked from registering without it.
alter table student_profiles
    alter column parent_mobile drop not null;

-- The single ordering slot is now labelled just "Recess" everywhere (was "Recess Time").
-- Renaming the row here is enough: every UI surface renders it via order.slotName /
-- window.slotName, so this one update fixes the display app-wide with no code changes.
update delivery_slots
set name = 'Recess'
where name = 'Recess Time';
