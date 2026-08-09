package com.school.canteen.controller;

import com.school.canteen.dto.payment.CalculatePricingRequest;
import com.school.canteen.dto.payment.CreatePaymentRequest;
import com.school.canteen.dto.payment.PaymentInitiationResponse;
import com.school.canteen.dto.payment.PaymentStatusResponse;
import com.school.canteen.dto.payment.RefundRequest;
import com.school.canteen.dto.payment.RefundResponse;
import com.school.canteen.dto.payment.VerifyPaymentRequest;
import com.school.canteen.enums.PaymentUseCase;
import com.school.canteen.exception.BadRequestException;
import com.school.canteen.payment.PaymentProviderType;
import com.school.canteen.pricing.PricingBreakdown;
import com.school.canteen.security.AppUserDetails;
import com.school.canteen.service.CreatePaymentCommand;
import com.school.canteen.service.PaymentService;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The provider-independent surface every client (web/mobile) actually talks to. No
 * Razorpay — or any other gateway — concept appears here or in anything this delegates to
 * except {@code payment/providers/*}.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** Non-binding preview so the UI can show a breakdown before the user commits. */
    @PostMapping("/calculate-pricing")
    public PricingBreakdown calculatePricing(@Valid @RequestBody CalculatePricingRequest request) {
        return paymentService.calculatePricing(request);
    }

    /**
     * Only WALLET_RECHARGE is accepted here — a CHECKOUT payment is always created
     * server-side from a trusted order total via {@code POST /api/orders}, never from a
     * client-supplied amount.
     */
    @PostMapping
    public PaymentInitiationResponse createPayment(@AuthenticationPrincipal AppUserDetails principal,
                                                    @Valid @RequestBody CreatePaymentRequest request) {
        if (request.useCase() != PaymentUseCase.WALLET_RECHARGE) {
            throw new BadRequestException(
                    "Only WALLET_RECHARGE payments can be created directly; checkout payments are created by placing an order");
        }
        UUID userId = principal.getUser().getId();
        return paymentService.createPayment(
                CreatePaymentCommand.walletRecharge(userId, request.amount(), request.idempotencyKey()));
    }

    @PostMapping("/{id}/verify")
    public PaymentStatusResponse verifyPayment(@AuthenticationPrincipal AppUserDetails principal,
                                               @PathVariable UUID id,
                                               @Valid @RequestBody VerifyPaymentRequest request) {
        return paymentService.verifyPayment(principal.getUser().getId(), id, request);
    }

    /** Dev-only: simulates a successful gateway callback when mock top-ups are allowed —
     *  see PaymentService.mockComplete. */
    @PostMapping("/{id}/mock-complete")
    public PaymentStatusResponse mockComplete(@AuthenticationPrincipal AppUserDetails principal,
                                              @PathVariable UUID id) {
        return paymentService.mockComplete(principal.getUser().getId(), id);
    }

    @GetMapping("/{id}")
    public PaymentStatusResponse getStatus(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id) {
        return paymentService.getPaymentStatus(principal.getUser().getId(), id);
    }

    /**
     * Called by the client the moment it knows checkout won't complete — the gateway
     * widget was dismissed, or the widget failed to load — so a payment (and any order it
     * started) doesn't sit around looking "placed" until PaymentExpirySweeper's 15-minute
     * sweep gets to it. Ownership-checked and idempotent, same as verify.
     */
    @PostMapping("/{id}/cancel")
    public PaymentStatusResponse cancel(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id) {
        return paymentService.cancelPayment(principal.getUser().getId(), id);
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasAnyRole('CANTEEN_ADMIN','SUB_ADMIN')")
    public RefundResponse refund(@PathVariable UUID id, @Valid @RequestBody RefundRequest request) {
        return paymentService.refundPayment(id, request);
    }

    /**
     * Called server-to-server by the provider, not by our own frontend/mobile client —
     * see the {@code permitAll()} carve-out in SecurityConfig. Authenticity comes entirely
     * from the signature PaymentService verifies, never from the caller being logged in.
     */
    @PostMapping("/webhooks/{provider}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void webhook(@PathVariable String provider,
                        @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
                        @RequestBody String rawBody) {
        PaymentProviderType providerType;
        try {
            providerType = PaymentProviderType.valueOf(provider.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unknown payment provider: " + provider);
        }
        paymentService.handleWebhook(providerType, rawBody, signature);
    }
}
