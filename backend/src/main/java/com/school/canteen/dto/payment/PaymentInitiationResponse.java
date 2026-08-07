package com.school.canteen.dto.payment;

import com.school.canteen.pricing.PricingBreakdown;
import java.util.UUID;

/**
 * @param providerOrderId  null when the payment was fully wallet-funded and settled
 *                         immediately — nothing to check out with a provider widget for.
 * @param providerKeyId    the provider's public key, for launching its checkout widget.
 * @param status           PaymentTxnStatus as a string ("PENDING" awaiting the gateway,
 *                         or "PAID" already — the client should skip the checkout widget
 *                         entirely when it sees PAID).
 */
public record PaymentInitiationResponse(
        UUID paymentId,
        String status,
        String providerOrderId,
        String providerKeyId,
        PricingBreakdown pricing) {
}
