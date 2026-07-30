-- Refresh tokens become server-side state so they can actually be revoked.
-- Previously they were stateless JWTs valid for 7 days with no way to invalidate them:
-- a leaked token meant a week of access that nobody could cut off.
-- Only the SHA-256 hash is stored, so a database leak does not expose usable tokens.
create table refresh_tokens (
    id          uuid         primary key,
    user_id     uuid         not null,
    token_hash  varchar(64)  not null,
    expires_at  timestamptz  not null,
    revoked_at  timestamptz,
    created_at  timestamptz  not null,
    updated_at  timestamptz  not null,
    constraint uq_refresh_token_hash unique (token_hash),
    constraint fk_refresh_token_user foreign key (user_id) references users (id) on delete cascade
);

create index idx_refresh_token_user on refresh_tokens (user_id);
create index idx_refresh_token_expiry on refresh_tokens (expires_at);
