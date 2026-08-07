package com.school.canteen.dto.payment;

import com.school.canteen.enums.PaymentMode;
import com.school.canteen.enums.PaymentUseCase;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * A non-binding preview — nothing is created or charged. The real charge always
 * re-derives its amount server-side at creation time (recharge amount from the request
 * that creates it; checkout subtotal from the order), so a client can never get a
 * favourable price here and have it honoured at charge time.
 *
 * @param amount              recharge amount (WALLET_RECHARGE) or order subtotal (CHECKOUT)
 * @param paymentMode         required for CHECKOUT, ignored for WALLET_RECHARGE
 * @param walletAmountAvailable how much wallet the caller would apply; ignored unless
 *                              paymentMode=WALLET_PLUS_GATEWAY
 */
public record CalculatePricingRequest(
        @NotNull PaymentUseCase useCase,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        PaymentMode paymentMode,
        BigDecimal walletAmountAvailable) {
}
