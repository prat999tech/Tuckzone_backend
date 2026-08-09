-- student_mobile duplicated the student's own login `mobile` on the user row with no
-- distinct purpose downstream (never read by order placement, delivery, or
-- notifications — confirmed by search before this migration was written). Dropping it
-- removes the confusing duplicate "phone number" field from registration.
alter table student_profiles drop column if exists student_mobile;
