package com.school.canteen.dto.wallet;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Everything the client needs to open the payment checkout. In a real integration the
 * frontend hands gatewayOrderId + gatewayKeyId to the Razorpay widget.
 */
public record TopupInitResponse(
        UUID topupId,
        String gatewayOrderId,
        BigDecimal amount,
        String currency,
        String gatewayKeyId) {
}
