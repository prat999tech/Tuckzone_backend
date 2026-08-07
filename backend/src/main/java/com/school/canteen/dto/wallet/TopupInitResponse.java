package com.school.canteen.dto.wallet;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Everything the client needs to open the payment checkout. The frontend hands
 * gatewayOrderId + gatewayKeyId to the Razorpay widget.
 *
 * @param amount      the wallet credit amount requested — what the wallet will actually
 *                    receive, never including platformFee (see Payment#getPlatformFee).
 * @param platformFee added on top of amount for what the customer actually pays through
 *                    the gateway; zero unless an admin has enabled a WALLET_RECHARGE fee.
 * @param grandTotal  amount + platformFee — what the gateway widget actually charges.
 */
public record TopupInitResponse(
        UUID topupId,
        String gatewayOrderId,
        BigDecimal amount,
        String currency,
        String gatewayKeyId,
        BigDecimal platformFee,
        BigDecimal grandTotal) {
}
