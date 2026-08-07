package com.school.canteen.dto.payment;

import com.school.canteen.enums.PaymentUseCase;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Public entry point for creating a payment directly (as opposed to through
 * {@code POST /api/orders}, which creates a CHECKOUT payment server-side once it has
 * resolved a trusted order total). Only WALLET_RECHARGE is accepted here — a client can
 * always be trusted for "how much do I want to add to my own wallet", but never for "how
 * much does this order cost", so CHECKOUT payments are always created from a
 * server-computed amount, never from a request body.
 */
public record CreatePaymentRequest(
        @NotNull PaymentUseCase useCase,
        @NotNull @DecimalMin("1.00") @Digits(integer = 8, fraction = 2) BigDecimal amount,
        @Size(max = 80) String idempotencyKey) {
}
