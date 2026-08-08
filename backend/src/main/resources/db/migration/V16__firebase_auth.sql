-- Firebase Authentication support, additive alongside the existing password/OTP system.
-- password_hash becomes nullable: a user who registers via Firebase (phone or email) never
-- sets one. Existing rows are untouched and keep their password_hash exactly as before.
alter table users alter column password_hash drop not null;

-- Nullable + unique: most rows won't have one (existing password users never link unless
-- they choose to), but no two users can ever claim the same Firebase identity.
alter table users add column firebase_uid varchar(128);
alter table users add constraint uq_users_firebase_uid unique (firebase_uid);
