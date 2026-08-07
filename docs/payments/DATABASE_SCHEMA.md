# Database Schema

Migration: `backend/src/main/resources/db/migration/V15__payment_platform.sql`.

`wallet_topups`/`WalletTopup` is **not** touched by this migration — it's left exactly as
it was, frozen as historical data. New code never writes to it again; `payments` replaces
it going forward.

## `payments`

The canonical ledger for every payment attempt — wallet recharge and order checkout alike.

| Column | Type | Notes |
|---|---|---|
| `id` | uuid PK | |
| `user_id` | uuid | FK `users` |
| `use_case` | varchar(20) | `WALLET_RECHARGE`, `CHECKOUT`, `SUBSCRIPTION` (reserved, unused) |
| `reference_type` | varchar(30) | e.g. `"ORDER"`; null for recharge |
| `reference_id` | varchar(64) | e.g. the order id as a string |
| `subtotal` | numeric(12,2) | |
| `platform_fee` | numeric(12,2) | default 0 — this app's revenue, never the wallet's |
| `discount` | numeric(12,2) | default 0 — future coupon hook, always 0 today |
| `tax` | numeric(12,2) | default 0 — future GST hook, always 0 today |
| `wallet_used` | numeric(12,2) | default 0 |
| `gateway_amount` | numeric(12,2) | default 0 |
| `grand_total` | numeric(12,2) | subtotal − discount + tax + platform_fee |
| `currency` | varchar(3) | default `INR` |
| `provider` | varchar(20) | `MOCK`, `RAZORPAY` |
| `provider_order_id` | varchar(80) | **unique** — idempotency key for settlement |
| `provider_payment_id` | varchar(80) | set on settlement |
| `signature` | varchar(255) | kept for audit, never re-derived from |
| `status` | varchar(20) | `PENDING`, `PAID`, `FAILED`, `REFUNDED`, `PARTIALLY_REFUNDED` |
| `idempotency_key` | varchar(80) | client-supplied, optional |
| `metadata` | jsonb | unused today, reserved |
| `created_at`, `updated_at` | timestamptz | |

Indexes: `(user_id, created_at)`, `(use_case, status)`, `(reference_type, reference_id)`,
partial unique `(user_id, idempotency_key) where idempotency_key is not null`.

No separate `pricing_breakdown` table — the breakdown is a 1:1 snapshot of a payment, so
it's these columns directly, not duplicated data. `PricingBreakdown` is a Java record
(service-layer value object and API response shape), not a table.

## `refunds`

| Column | Type | Notes |
|---|---|---|
| `id` | uuid PK | |
| `payment_id` | uuid | FK `payments` |
| `wallet_amount` | numeric(12,2) | default 0 |
| `gateway_amount` | numeric(12,2) | default 0 |
| `total_amount` | numeric(12,2) | wallet_amount + gateway_amount, > 0 |
| `reason` | varchar(255) | |
| `status` | varchar(20) | `PENDING`, `PROCESSED`, `FAILED` |
| `provider_refund_id` | varchar(80) | |
| `created_at`, `updated_at` | timestamptz | |

## `platform_fee_settings`

One row per `PaymentUseCase`, seeded by V15 for `WALLET_RECHARGE` and `CHECKOUT`, both
`enabled=false`.

| Column | Type | Notes |
|---|---|---|
| `id` | uuid PK | |
| `use_case` | varchar(20) | **unique** |
| `enabled` | boolean | default false |
| `fee_type` | varchar(20) | `PERCENTAGE`, `FIXED` |
| `fee_value` | numeric(8,4) | percent (e.g. 2.00 = 2%) or a currency amount |
| `min_fee` | numeric(12,2) | nullable |
| `max_fee` | numeric(12,2) | nullable |
| `created_at`, `updated_at` | timestamptz | |

## `orders` (altered)

One new nullable column: `payment_id uuid references payments(id)`. Set only when the
order used the gateway/split path; null for the original wallet-only fast path, which is
otherwise completely unchanged.
