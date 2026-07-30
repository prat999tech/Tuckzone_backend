-- OTP moves from SMS to email.
--
-- Rationale: SMS in India requires DLT registration (business entity + sender id + exact
-- template wording pre-approved by a telecom aggregator) and a paid per-message gateway.
-- Email needs only an SMTP account, works with any provider, and costs nothing.

-- Outstanding codes are ephemeral (5-minute TTL) and are keyed by the column being
-- repurposed, so clearing them is the correct migration rather than trying to translate
-- mobile numbers into email addresses.
delete from otp_codes;

alter table otp_codes rename column mobile to email;
alter table otp_codes alter column email type varchar(180);

drop index if exists idx_otp_lookup;
create index idx_otp_lookup on otp_codes (email, purpose, created_at desc);

-- Registration now sends a verification code to the address the user supplied.
-- Defaults to false; existing accounts are backfilled as verified below so nobody who
-- already registered is retroactively told to verify.
alter table users add column email_verified boolean not null default false;
update users set email_verified = true;

-- Mobile is no longer a login identity, so it no longer has to be unique.
-- This removes a real-world friction point: siblings, or a young student and their parent,
-- legitimately share one phone number.
alter table users drop constraint if exists uq_users_mobile;
