package com.school.canteen.payment;

public record ProviderRefundCommand(String providerPaymentId, long amountPaise, String reason) {
}
