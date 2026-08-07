# API Reference

All endpoints are under `/api`. Authenticated ones require the standard `Authorization:
Bearer <accessToken>` header (see the main auth docs) — omitted below for brevity.

## Generic payment endpoints (`/api/payments`)

### `POST /payments/calculate-pricing`
Non-binding preview. Nothing is created or charged — the real charge always re-derives
its amount server-side.

```json
// Request
{ "useCase": "WALLET_RECHARGE", "amount": 1000.00 }
// or
{ "useCase": "CHECKOUT", "amount": 250.00, "paymentMode": "WALLET_PLUS_GATEWAY", "walletAmountAvailable": 100.00 }

// Response — PricingBreakdown
{ "subtotal": 1000.00, "platformFee": 0, "discount": 0, "tax": 0,
  "walletUsed": 0, "gatewayAmount": 1000.00, "grandTotal": 1000.00, "currency": "INR" }
```

### `POST /payments`
Only `useCase: "WALLET_RECHARGE"` is accepted here — `CHECKOUT` payments are always
created server-side by `POST /orders`, never from a client-supplied amount.

```json
// Request
{ "useCase": "WALLET_RECHARGE", "amount": 1000.00, "idempotencyKey": "optional-client-key" }

// Response — PaymentInitiationResponse
{ "paymentId": "...", "status": "PENDING", "providerOrderId": "order_...",
  "providerKeyId": "rzp_test_...", "pricing": { ... } }
```

### `POST /payments/{id}/verify`
```json
// Request
{ "providerOrderId": "order_...", "providerPaymentId": "pay_...", "signature": "..." }
// Response — PaymentStatusResponse
{ "paymentId": "...", "useCase": "WALLET_RECHARGE", "status": "PAID",
  "provider": "RAZORPAY", "providerPaymentId": "pay_...", "pricing": { ... }, "createdAt": "..." }
```

### `POST /payments/{id}/mock-complete`
Dev-only. Self-signs a synthetic payment and settles it the same way a real callback
would. 400 if `app.payment.allow-mock-topup=false`. No request body.

### `GET /payments/{id}`
Returns the same `PaymentStatusResponse` shape as verify. Ownership-checked — 404 for
another user's payment.

### `POST /payments/{id}/refund` — admin only (`CANTEEN_ADMIN`/`SUB_ADMIN`)
```json
// Request — amount omitted = full refund of whatever remains unrefunded
{ "amount": 100.00, "reason": "Order rejected" }
// Response
{ "refundId": "...", "walletAmount": 80.00, "gatewayAmount": 20.00, "totalAmount": 100.00, "status": "PROCESSED" }
```

### `POST /payments/webhooks/{provider}` — public, no auth
Called server-to-server by the provider. `{provider}` is `razorpay` (matches
`PaymentProviderType`). Authenticity comes entirely from the signature verified inside
`PaymentService.handleWebhook`, not from a JWT.

## Wallet endpoints (`/api/wallet`) — unchanged contract

Same four endpoints as before this work (`GET /wallet`, `GET /wallet/transactions`,
`POST /wallet/topup`, `POST /wallet/topup/verify`, `POST /wallet/topup/mock-complete`).
`TopupInitResponse` gained two additive fields: `platformFee`, `grandTotal`.

## Order checkout (`POST /orders`) — additive field

`PlaceOrderRequest` gained one optional field: `paymentMode` (`WALLET_ONLY` / omitted =
unchanged wallet-only behaviour; `GATEWAY_ONLY` / `WALLET_PLUS_GATEWAY` = new gateway/split
path). `OrderResponse` gained one optional field: `payment` (a `PaymentInitiationResponse`,
non-null only when the client still needs to complete a gateway checkout).

## Admin: platform fee settings (`/api/admin/payment-settings`) — `CANTEEN_ADMIN` only

- `GET /admin/payment-settings` → `List<PlatformFeeSettingsResponse>` (one per use case)
- `PUT /admin/payment-settings/{useCase}` with body:
  ```json
  { "enabled": true, "feeType": "PERCENTAGE", "feeValue": 2.00, "minFee": null, "maxFee": 50.00 }
  ```

## Admin: payment revenue reporting (`/api/admin/reports/payments`) — `CANTEEN_ADMIN` only

- `GET /admin/reports/payments` → `PlatformRevenueResponse` (today/month/total, by
  provider, by use case — platform fee revenue only, never gross payment volume)
- `GET /admin/reports/payments/export?from=YYYY-MM-DD&to=YYYY-MM-DD` → CSV of every
  payment attempt (any status) in range
