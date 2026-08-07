package com.school.canteen.payment;

public record ProviderVerifyPaymentCommand(String providerOrderId, String providerPaymentId, String signature) {
}
