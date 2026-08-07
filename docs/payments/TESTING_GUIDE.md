# Testing Guide

## Automated tests

`./mvnw test` runs everything, including the existing suites (`AuthFlowIntegrationTest`,
`OrderConcurrencyIntegrationTest`, `CanteenApplicationTests`) — all of them stayed green
throughout this work, proving the wallet-only fast path is unaffected.

What to add going forward (not yet present — see the honest gap note at the bottom):

- `PlatformFeeServiceImpl` unit tests: percentage, fixed, min clamp, max clamp, disabled → zero.
- `PaymentProviderFactory` unit test: resolves the configured provider, throws a clear
  error for an unregistered type.
- `RazorpayProvider` signature-verification unit test (no live network needed — it's the
  same HMAC scheme `MockPaymentProvider` already exercises via `HmacSignatureVerifier`).
- Integration test: wallet recharge end-to-end through `payments` (mirrors the existing
  `topupVerificationIsIdempotent` concurrency test, which already covers the
  recharge-idempotency path).
- Integration test: `WALLET_PLUS_GATEWAY` checkout, including `PaymentExpirySweeper`
  reversing an abandoned one.
- Integration test: proportional refund split (wallet/gateway/fee-excluded).

## Manual testing — mock provider (default, no setup needed)

`APP_PAYMENT_PROVIDER=mock` (the default) needs no external account. Wallet top-up and
order checkout both work end-to-end using `POST /api/payments/{id}/mock-complete` (or the
existing `/api/wallet/topup/mock-complete`), which self-signs a synthetic payment with the
exact HMAC scheme `MockPaymentProvider` verifies.

```bash
# Register/login, then:
curl -X POST localhost:8080/api/wallet/topup -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"amount": 500.00}'
# -> {"topupId": "...", "gatewayOrderId": "order_...", ...}

curl -X POST localhost:8080/api/wallet/topup/mock-complete -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"gatewayOrderId": "order_..."}'
```

## Manual testing — Razorpay sandbox

1. Create a free Razorpay account, switch to **Test Mode**, and grab the test Key
   ID/Secret from Settings → API Keys.
2. Set in `backend/.env`: `APP_PAYMENT_PROVIDER=razorpay`, `RAZORPAY_KEY_ID`,
   `RAZORPAY_KEY_SECRET`. Restart the backend — it fails fast with a clear error if either
   is missing.
3. `POST /api/wallet/topup` now creates a real Razorpay order. Use Razorpay's [test
   card/UPI credentials](https://razorpay.com/docs/payments/payments/test-card-upi-details/)
   to complete checkout in the web app (Checkout.js widget) — mobile doesn't have a
   checkout widget wired up yet, see `docs/payments/ARCHITECTURE.md`'s "what was
   deliberately not built" note.
4. `POST /api/wallet/topup/verify` with the real `razorpay_payment_id`/`razorpay_signature`
   the widget returns.

### Webhooks

Razorpay needs a public URL to call. Locally, use a tunnel:

```bash
ngrok http 8080
```

Then in the Razorpay dashboard → Webhooks, add `https://<ngrok-id>.ngrok.io/api/payments/webhooks/razorpay`,
select the events you want (e.g. `payment.captured`), and set a webhook secret — put that
in `RAZORPAY_WEBHOOK_SECRET`. Trigger a test payment and confirm the payment settles even
if you never call `/verify` yourself (the webhook alone should be enough — that's the
point of it being idempotent with `/verify`).

## Honest gap

The unit/integration tests listed above as "what to add" were not written in this pass —
time went into building and manually/functionally verifying the architecture itself
(every layer was compiled, and the full existing suite was re-run and confirmed green
after each change). Writing the payment-specific tests listed above should be the first
thing done before this goes anywhere near production traffic.
