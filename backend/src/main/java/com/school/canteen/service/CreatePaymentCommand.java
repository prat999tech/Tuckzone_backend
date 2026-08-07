package com.school.canteen.service;

import com.school.canteen.enums.PaymentMode;
import com.school.canteen.enums.PaymentUseCase;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Internal, server-trusted instruction to create a payment — never built directly from an
 * HTTP request body for CHECKOUT. {@code subtotal} for a CHECKOUT payment must come from a
 * value the server itself computed (an order's stored total), never from client input;
 * for WALLET_RECHARGE it is the user's own requested top-up amount, which is legitimately
 * client-supplied since there is nothing else for it to be validated against.
 */
public record CreatePaymentCommand(
        UUID userId,
        PaymentUseCase useCase,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal tax,
        PaymentMode paymentMode,
        BigDecimal walletAmountAvailable,
        String referenceType,
        String referenceId,
        String idempotencyKey) {

    /** Recharge has no discount/tax/wallet-funding concept — it is always 100% gateway. */
    public static CreatePaymentCommand walletRecharge(UUID userId, BigDecimal amount, String idempotencyKey) {
        return new CreatePaymentCommand(userId, PaymentUseCase.WALLET_RECHARGE, amount,
                BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO, null, null, idempotencyKey);
    }
}
