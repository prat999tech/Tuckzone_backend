package com.school.canteen.payment;

/**
 * The result of asking the gateway to create a payment order. The client uses orderId +
 * keyId to open the checkout; the gateway later returns a paymentId + signature we verify.
 */
public record GatewayOrder(
        String orderId,
        long amountPaise,
        String currency,
        String keyId) {
}
