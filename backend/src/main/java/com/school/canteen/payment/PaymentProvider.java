package com.school.canteen.payment;

/**
 * Abstraction over a payment gateway (Strategy pattern). {@code PaymentService} depends
 * only on this interface, resolved through {@link PaymentProviderFactory} — it never knows
 * which concrete gateway is active, and no provider SDK is imported anywhere outside
 * {@code payment/providers/}. Adding a new gateway means one new class implementing this
 * interface plus one new {@link PaymentProviderType} value; nothing else changes.
 */
public interface PaymentProvider {

    PaymentProviderType type();

    /** Creates a payment order/intent with the gateway for the caller to check out against. */
    ProviderOrder createOrder(ProviderCreateOrderCommand command);

    /**
     * Verifies a client-supplied payment callback. Implementations must verify
     * cryptographically (signature/HMAC) — never trust the callback's own "success" flag.
     */
    ProviderVerificationResult verifyPayment(ProviderVerifyPaymentCommand command);

    /** Refunds a previously captured payment through the gateway. */
    ProviderRefundResult refund(ProviderRefundCommand command);

    /** Verifies an inbound webhook's signature and extracts what it refers to. */
    WebhookVerificationResult verifyWebhookSignature(String rawBody, String signatureHeader);
}
