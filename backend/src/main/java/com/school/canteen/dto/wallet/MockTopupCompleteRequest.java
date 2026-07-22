package com.school.canteen.dto.wallet;

import jakarta.validation.constraints.NotBlank;

/**
 * Development-only helper request for the mock gateway flow. In real Razorpay/Cashfree
 * integration the frontend would receive paymentId + signature from the provider instead.
 */
public record MockTopupCompleteRequest(
        @NotBlank String gatewayOrderId) {
}
