package com.school.canteen.pricing;

import java.math.BigDecimal;

/**
 * The full price computation for one payment, backend-computed and never trusted from the
 * client. {@code subtotal - discount + tax + platformFee = grandTotal}, and
 * {@code walletUsed + gatewayAmount = grandTotal}. Mirrors the columns persisted on
 * {@link com.school.canteen.entity.Payment} 1:1 — this is that row's shape before it has
 * an id, not a separate concept.
 */
public record PricingBreakdown(
        BigDecimal subtotal,
        BigDecimal platformFee,
        BigDecimal discount,
        BigDecimal tax,
        BigDecimal walletUsed,
        BigDecimal gatewayAmount,
        BigDecimal grandTotal,
        String currency) {
}
