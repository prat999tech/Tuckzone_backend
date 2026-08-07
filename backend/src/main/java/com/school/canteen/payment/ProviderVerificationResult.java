package com.school.canteen.payment;

public record ProviderVerificationResult(boolean success, String failureReason) {

    public static ProviderVerificationResult ok() {
        return new ProviderVerificationResult(true, null);
    }

    public static ProviderVerificationResult failure(String reason) {
        return new ProviderVerificationResult(false, reason);
    }
}
