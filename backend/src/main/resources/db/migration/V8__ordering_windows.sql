-- Advance ordering control.
--
-- Customers order days ahead, so the canteen needs a switch that is independent of the
-- slot's normal cutoff time: stop taking orders early when the kitchen is at capacity, or
-- reopen (and extend the cutoff) after topping stock back up.
--
-- A missing row means "behave normally": ordering is allowed until the slot's cutoff.
-- Only dates the admin has actually touched get a row.
create table ordering_windows (
    id                   uuid         primary key,
    menu_date            date         not null,
    slot_id              uuid         not null,
    status               varchar(20)  not null,
    -- Overrides the slot's cutoff when set, which is what lets an admin reopen ordering
    -- after the normal deadline has already passed.
    override_cutoff_time time,
    reason               varchar(255),
    created_at           timestamptz  not null,
    updated_at           timestamptz  not null,
    constraint uq_ordering_window unique (menu_date, slot_id),
    constraint fk_ordering_window_slot foreign key (slot_id) references delivery_slots (id)
);

create index idx_ordering_window_date on ordering_windows (menu_date);
