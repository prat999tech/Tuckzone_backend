# Adding a New Payment Provider

Example: Cashfree. The same steps apply to PhonePe, Stripe, Juspay, Easebuzz, or anything
else — nothing outside these files changes.

## 1. Add the enum value

`backend/src/main/java/com/school/canteen/payment/PaymentProviderType.java`:

```java
public enum PaymentProviderType {
    MOCK,
    RAZORPAY,
    CASHFREE   // <- add this
}
```

## 2. Add the SDK dependency, scoped to nowhere else

In `backend/pom.xml`, add Cashfree's SDK. It must never be imported by anything outside
`payment/providers/`.

## 3. Implement `PaymentProvider`

`backend/src/main/java/com/school/canteen/payment/providers/CashfreeProvider.java`:

```java
@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "cashfree")
public class CashfreeProvider implements PaymentProvider {

    @Override
    public PaymentProviderType type() { return PaymentProviderType.CASHFREE; }

    @Override
    public ProviderOrder createOrder(ProviderCreateOrderCommand command) { /* SDK call */ }

    @Override
    public ProviderVerificationResult verifyPayment(ProviderVerifyPaymentCommand command) {
        /* Cashfree's own signature scheme — do NOT reuse HmacSignatureVerifier unless
           Cashfree's scheme is genuinely the same HMAC-SHA256(orderId|paymentId, secret)
           construction Razorpay's happens to be; check their docs, don't assume. */
    }

    @Override
    public ProviderRefundResult refund(ProviderRefundCommand command) { /* SDK call */ }

    @Override
    public WebhookVerificationResult verifyWebhookSignature(String rawBody, String signatureHeader) { /* ... */ }
}
```

`@ConditionalOnProperty` here is only a convenience default (unused providers still get
registered as beans and are simply never selected) — `PaymentProviderFactory` is what
actually decides which one is active; you could drop the annotation entirely and it would
still work correctly, just registering an always-present bean nobody resolves.

If the provider needs config beyond the generic `key-id`/`key-secret` on
`PaymentProperties` (e.g. a separate webhook secret, the way `RazorpayProperties` does),
add a small `CashfreeProperties` record the same way.

## 4. Wire the env var and set it

```bash
APP_PAYMENT_PROVIDER=cashfree
CASHFREE_KEY_ID=...
CASHFREE_KEY_SECRET=...
```

## 5. Done

`PaymentService`, `WalletServiceImpl`, `OrderServiceImpl`, every controller, the pricing
engine, the admin settings page, the reporting — none of them change. They all depend on
`PaymentProvider`/`PaymentProviderFactory`, never on a concrete provider.

## What to verify before trusting it

- Order creation and refund against the provider's real sandbox — nothing about this can
  be verified without hitting their actual API.
- The provider's real signature scheme, from their docs — do not assume it matches
  Razorpay's just because it's convenient; the whole reason `HmacSignatureVerifier` is
  shared here is that it was confirmed byte-for-byte against Razorpay's own SDK source
  (`Utils.java`), not assumed.
- Webhook payload shape — `verifyWebhookSignature` needs to extract `eventType`,
  `providerOrderId`, `providerPaymentId` from whatever JSON structure that provider
  actually sends.
