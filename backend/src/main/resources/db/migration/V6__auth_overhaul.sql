-- The canteen is vendor-operated: the school owns nothing here, so the school-
-- administrator role and its registration-approval gate are removed entirely.
-- Existing school admins become canteen admins rather than being deleted, which avoids
-- breaking any rows that reference them.
update users set role = 'CANTEEN_ADMIN' where role = 'SCHOOL_ADMIN';

-- Account status collapses to ACTIVE / DISABLED now that nothing needs approving.
-- Anyone previously waiting for approval becomes usable; rejected accounts stay blocked.
update users set status = 'ACTIVE'   where status in ('PENDING', 'APPROVED');
update users set status = 'DISABLED' where status = 'REJECTED';

-- Mobile becomes a login identity for OTP, so it must identify exactly one account.
-- NOTE: this fails loudly if two accounts share a number — fix the data before deploying.
-- Students too young for their own phone do not need their own account: a parent links
-- the child and orders on their behalf.
alter table users add constraint uq_users_mobile unique (mobile);

-- Extra registration detail requested for students.
alter table student_profiles add column seat_number varchar(20);

-- One-time passcodes. Only a hash is stored, so the database never holds a usable code.
-- Rows are kept (not deleted on use) so a consumed code can never be replayed.
create table otp_codes (
    id          uuid          primary key,
    mobile      varchar(20)   not null,
    purpose     varchar(30)   not null,
    code_hash   varchar(100)  not null,
    expires_at  timestamptz   not null,
    consumed_at timestamptz,
    attempts    integer       not null default 0,
    created_at  timestamptz   not null,
    updated_at  timestamptz   not null,
    constraint chk_otp_attempts_non_negative check (attempts >= 0)
);

-- Supports "most recent live code for this mobile + purpose".
create index idx_otp_lookup on otp_codes (mobile, purpose, created_at desc);
create index idx_otp_expiry on otp_codes (expires_at);
