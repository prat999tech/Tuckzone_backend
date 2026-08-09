package com.school.canteen.service;

import com.school.canteen.dto.payment.CalculatePricingRequest;
import com.school.canteen.dto.payment.PaymentInitiationResponse;
import com.school.canteen.dto.payment.PaymentStatusResponse;
import com.school.canteen.dto.payment.RefundRequest;
import com.school.canteen.dto.payment.RefundResponse;
import com.school.canteen.dto.payment.VerifyPaymentRequest;
import com.school.canteen.payment.PaymentProviderType;
import com.school.canteen.pricing.PricingBreakdown;
import java.util.UUID;

/**
 * The single orchestrator every payment-shaped feature goes through. Deliberately thin on
 * arithmetic and thin on gateway detail: money is computed by {@code PricingService}, the
 * gateway is talked to through {@code PaymentProviderFactory} — this class never imports a
 * provider SDK and never computes a fee itself. Wallet recharge and order checkout are
 * both just {@link CreatePaymentCommand}s with a different {@code PaymentUseCase}.
 */
public interface PaymentService {

    PaymentInitiationResponse createPayment(CreatePaymentCommand command);

    /** Ownership-checked: verifying someone else's payment id always fails. */
    PaymentStatusResponse verifyPayment(UUID userId, UUID paymentId, VerifyPaymentRequest request);

    /**
     * Dev-only helper that simulates a successful gateway callback when
     * {@code app.payment.allow-mock-topup=true} — the general-purpose form of what
     * {@code WalletService.mockCompleteTopup} exposes for wallet recharges specifically.
     * Self-signs a synthetic provider payment id with the same HMAC scheme
     * {@link com.school.canteen.payment.providers.MockPaymentProvider} verifies, then
     * runs through the exact same settlement path a real callback would.
     */
    PaymentStatusResponse mockComplete(UUID userId, UUID paymentId);

    PaymentStatusResponse getPaymentStatus(UUID userId, UUID paymentId);

    /**
     * Abandons a payment the customer backed out of, releasing everything it held.
     *
     * Called when the gateway sheet is dismissed. Without it the wallet portion — which is
     * charged up front — stayed debited until the 15-minute expiry sweep, which is exactly
     * what "money deducted even though I cancelled" looks like to a customer.
     */
    PaymentStatusResponse cancelPayment(UUID userId, UUID paymentId);

    /** Admin-only; not ownership-scoped to a single user. */
    RefundResponse refundPayment(UUID paymentId, RefundRequest request);

    /** Non-binding preview — computes but persists and charges nothing. */
    PricingBreakdown calculatePricing(CalculatePricingRequest request);

    /**
     * Reconciles a provider webhook. Never trusts the payload's own status field — the
     * signature is checked first, and even then only re-confirms what
     * {@link #verifyPayment} would have: the stored amount and the provider's
     * cryptographic signature. Idempotent against both a client verify call and a webhook
     * arriving in either order, or the same webhook being redelivered.
     */
    void handleWebhook(PaymentProviderType providerType, String rawBody, String signatureHeader);
}
