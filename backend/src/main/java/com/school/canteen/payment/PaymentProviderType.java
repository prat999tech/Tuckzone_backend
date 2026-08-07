package com.school.canteen.payment;

/**
 * Every {@link PaymentProvider} implementation this app knows about. Adding a new provider
 * means adding one value here, one class implementing {@link PaymentProvider}, and nothing
 * else — see docs/payments/ADDING_A_NEW_PROVIDER.md.
 */
public enum PaymentProviderType {
    MOCK,
    RAZORPAY
}
