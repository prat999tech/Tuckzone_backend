package com.school.canteen.dto.wallet;

import jakarta.validation.constraints.NotBlank;

/** Sent back after the user pays; the gateway supplies paymentId + signature. */
public record VerifyTopupRequest(
        @NotBlank String gatewayOrderId,
        @NotBlank String gatewayPaymentId,
        @NotBlank String signature) {
}
