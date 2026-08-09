package com.school.canteen.payment;

/** Outcome of checking an inbound webhook's signature and pulling out what it refers to. */
public record WebhookVerificationResult(boolean valid, WebhookOutcome outcome, String providerOrderId,
                                        String providerPaymentId) {

    public static WebhookVerificationResult invalid() {
        return new WebhookVerificationResult(false, null, null, null);
    }
}
