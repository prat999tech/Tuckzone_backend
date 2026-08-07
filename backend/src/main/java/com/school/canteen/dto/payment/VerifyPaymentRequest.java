package com.school.canteen.dto.payment;

import jakarta.validation.constraints.NotBlank;

public record VerifyPaymentRequest(
        @NotBlank String providerOrderId,
        @NotBlank String providerPaymentId,
        @NotBlank String signature) {
}
