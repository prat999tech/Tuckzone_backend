package com.school.canteen.payment;

/**
 * Provider-neutral meaning of an inbound webhook event, decided by the provider (which
 * alone knows its own raw event names, e.g. Razorpay's {@code payment.captured}) so
 * {@code PaymentService} never has to pattern-match a gateway-specific string — same
 * reasoning as every other provider-specific concern staying inside {@code payment/providers/*}.
 */
public enum WebhookOutcome {
    /** The referenced payment succeeded — settle it, same as a client verify call. */
    SUCCESS,
    /** The referenced payment failed or was cancelled at the gateway — void it and its order. */
    FAILURE,
    /** An event this app doesn't act on (e.g. authorized-but-not-captured, a refund event) —
     *  acknowledge and ignore rather than guessing. */
    IGNORE
}
