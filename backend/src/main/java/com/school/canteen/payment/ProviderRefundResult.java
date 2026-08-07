package com.school.canteen.payment;

public record ProviderRefundResult(boolean success, String providerRefundId, String failureReason) {

    public static ProviderRefundResult success(String providerRefundId) {
        return new ProviderRefundResult(true, providerRefundId, null);
    }

    public static ProviderRefundResult failure(String reason) {
        return new ProviderRefundResult(false, null, reason);
    }
}
