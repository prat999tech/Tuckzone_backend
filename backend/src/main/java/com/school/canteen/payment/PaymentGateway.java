package com.school.canteen.payment;

/**
 * Abstraction over the payment provider (Strategy pattern). The wallet service depends on
 * this interface, never on a concrete provider — so the mock used in development and a real
 * Razorpay implementation are interchangeable without touching business logic.
 */
public interface PaymentGateway {

    /** Create a payment order for the given amount (in paise). */
    GatewayOrder createOrder(long amountPaise, String receipt);

    /** Verify the provider's payment callback signature. True = genuine, paid. */
    boolean verifyPayment(String orderId, String paymentId, String signature);
}
