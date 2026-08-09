-- Replaces the parent-child-link flow (link an existing student account by admission
-- number + parent mobile) with a lightweight "ward" a parent enters directly: name,
-- class, section, no login and no admission number required. parent_child_links is left
-- in place untouched (harmless, historical, reversible) rather than dropped.
create table wards (
    id            uuid          primary key,
    parent_user_id uuid         not null,
    name          varchar(255)  not null,
    student_class varchar(50)   not null,
    section       varchar(50)   not null,
    created_at    timestamptz   not null,
    updated_at    timestamptz   not null,
    constraint fk_ward_parent foreign key (parent_user_id) references users (id)
);
create index idx_ward_parent on wards (parent_user_id);

-- Carry forward every parent's already-linked children as wards, so nobody's existing
-- data silently disappears when the frontends switch away from the old linking flow.
insert into wards (id, parent_user_id, name, student_class, section, created_at, updated_at)
select gen_random_uuid(), pcl.parent_user_id, u.full_name, sp.student_class, sp.section,
       now(), now()
from parent_child_links pcl
join student_profiles sp on sp.id = pcl.student_profile_id
join users u on u.id = sp.user_id;

-- recipient_name/delivery_location are already snapshotted onto the order at placement
-- time (see Order entity), so a parent removing a ward later must not be blocked by, or
-- corrupt, that order's history — on delete set null, matching the same precedent used
-- for menu_items in V14.
alter table orders add column beneficiary_ward_id uuid;
alter table orders
    add constraint fk_orders_ward foreign key (beneficiary_ward_id) references wards (id) on delete set null;
