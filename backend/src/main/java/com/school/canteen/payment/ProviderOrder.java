package com.school.canteen.payment;

/** The result of asking a provider to open an order. The client uses providerOrderId +
 *  providerKeyId to launch checkout; the provider later returns a paymentId + signature
 *  that {@link PaymentProvider#verifyPayment} checks. */
public record ProviderOrder(String providerOrderId, long amountPaise, String currency, String providerKeyId) {
}
