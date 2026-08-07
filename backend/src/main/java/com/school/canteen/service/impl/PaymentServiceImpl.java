package com.school.canteen.service.impl;

import com.school.canteen.config.PaymentProperties;
import com.school.canteen.config.RateLimitProperties;
import com.school.canteen.dto.payment.CalculatePricingRequest;
import com.school.canteen.dto.payment.PaymentInitiationResponse;
import com.school.canteen.dto.payment.PaymentStatusResponse;
import com.school.canteen.dto.payment.RefundRequest;
import com.school.canteen.dto.payment.RefundResponse;
import com.school.canteen.dto.payment.VerifyPaymentRequest;
import com.school.canteen.entity.Order;
import com.school.canteen.entity.Payment;
import com.school.canteen.entity.Refund;
import com.school.canteen.entity.User;
import com.school.canteen.enums.NotificationEvent;
import com.school.canteen.enums.PaymentMode;
import com.school.canteen.enums.PaymentStatus;
import com.school.canteen.enums.PaymentTxnStatus;
import com.school.canteen.enums.PaymentUseCase;
import com.school.canteen.enums.RefundStatus;
import com.school.canteen.exception.BadRequestException;
import com.school.canteen.exception.ResourceNotFoundException;
import com.school.canteen.payment.HmacSignatureVerifier;
import com.school.canteen.payment.PaymentProvider;
import com.school.canteen.payment.PaymentProviderFactory;
import com.school.canteen.payment.PaymentProviderType;
import com.school.canteen.payment.ProviderCreateOrderCommand;
import com.school.canteen.payment.ProviderOrder;
import com.school.canteen.payment.ProviderRefundCommand;
import com.school.canteen.payment.ProviderRefundResult;
import com.school.canteen.payment.ProviderVerificationResult;
import com.school.canteen.payment.ProviderVerifyPaymentCommand;
import com.school.canteen.payment.WebhookVerificationResult;
import com.school.canteen.pricing.PricingBreakdown;
import com.school.canteen.pricing.PricingService;
import com.school.canteen.repository.OrderRepository;
import com.school.canteen.repository.PaymentRepository;
import com.school.canteen.repository.RefundRepository;
import com.school.canteen.repository.UserRepository;
import com.school.canteen.security.RateLimiterService;
import com.school.canteen.service.CreatePaymentCommand;
import com.school.canteen.service.NotificationService;
import com.school.canteen.service.PaymentService;
import com.school.canteen.service.WalletService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The payment orchestrator. Deliberately does not compute money (delegates to
 * {@link PricingService}) and does not import a provider SDK (delegates to
 * {@link PaymentProviderFactory}) — see the interface javadoc.
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PricingService pricingService;
    private final PaymentProviderFactory paymentProviderFactory;
    private final WalletService walletService;
    private final NotificationService notificationService;
    private final RateLimiterService rateLimiter;
    private final RateLimitProperties rateLimitProperties;
    private final PaymentProperties paymentProperties;

    public PaymentServiceImpl(PaymentRepository paymentRepository, RefundRepository refundRepository,
                              OrderRepository orderRepository, UserRepository userRepository,
                              PricingService pricingService, PaymentProviderFactory paymentProviderFactory,
                              WalletService walletService, NotificationService notificationService,
                              RateLimiterService rateLimiter, RateLimitProperties rateLimitProperties,
                              PaymentProperties paymentProperties) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.pricingService = pricingService;
        this.paymentProviderFactory = paymentProviderFactory;
        this.walletService = walletService;
        this.notificationService = notificationService;
        this.rateLimiter = rateLimiter;
        this.rateLimitProperties = rateLimitProperties;
        this.paymentProperties = paymentProperties;
    }

    @Override
    @Transactional
    public PaymentInitiationResponse createPayment(CreatePaymentCommand command) {
        if (command.idempotencyKey() != null) {
            var existing = paymentRepository.findByUser_IdAndIdempotencyKey(command.userId(), command.idempotencyKey());
            if (existing.isPresent()) {
                return toInitiationResponse(existing.get());
            }
        }
        rateLimiter.checkAndConsume("payment-create:" + command.userId(),
                rateLimitProperties.paymentCreateAttemptsPerWindow(),
                Duration.ofMinutes(rateLimitProperties.windowMinutes()),
                "Too many payment attempts. Please wait before trying again.");

        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        PricingBreakdown pricing = switch (command.useCase()) {
            case WALLET_RECHARGE -> pricingService.calculateWalletRechargePricing(command.subtotal());
            case CHECKOUT -> pricingService.calculateCheckoutPricing(command.subtotal(), command.discount(),
                    command.tax(), command.paymentMode(), command.walletAmountAvailable());
            case SUBSCRIPTION -> throw new BadRequestException("Subscription payments are not supported yet");
        };

        Payment payment = new Payment();
        payment.setUser(user);
        payment.setUseCase(command.useCase());
        payment.setReferenceType(command.referenceType());
        payment.setReferenceId(command.referenceId());
        payment.setSubtotal(pricing.subtotal());
        payment.setPlatformFee(pricing.platformFee());
        payment.setDiscount(pricing.discount());
        payment.setTax(pricing.tax());
        payment.setWalletUsed(pricing.walletUsed());
        payment.setGatewayAmount(pricing.gatewayAmount());
        payment.setGrandTotal(pricing.grandTotal());
        payment.setCurrency(pricing.currency());
        payment.setProvider(paymentProviderFactory.active().type());
        payment.setStatus(PaymentTxnStatus.PENDING);
        payment.setIdempotencyKey(command.idempotencyKey());
        // provider_order_id is NOT NULL + unique; this placeholder is immediately replaced
        // below (with a real provider order id, or a synthetic wallet-only marker) before
        // any other transaction could observe it.
        payment.setProviderOrderId("pending_" + UUID.randomUUID());
        payment = paymentRepository.save(payment);

        // Wallet portion is charged synchronously, same as the pre-existing wallet-only
        // flow — there is no external dependency for it, so there is no reason to make the
        // customer wait on a gateway round trip for money that's already available.
        // Compensation if the gateway leg below never completes is PaymentExpirySweeper's
        // job (releases this back to the wallet), not this method's.
        if (pricing.walletUsed().signum() > 0) {
            walletService.debit(command.userId(), pricing.walletUsed(), "PAYMENT", payment.getId().toString(),
                    describeWalletDebit(command.useCase()));
        }

        if (pricing.gatewayAmount().signum() > 0) {
            ProviderOrder providerOrder = paymentProviderFactory.active().createOrder(new ProviderCreateOrderCommand(
                    toPaise(pricing.gatewayAmount()), pricing.currency(), receiptFor(payment), Map.of()));
            payment.setProviderOrderId(providerOrder.providerOrderId());
            paymentRepository.save(payment);
            return new PaymentInitiationResponse(payment.getId(), payment.getStatus().name(),
                    providerOrder.providerOrderId(), providerOrder.providerKeyId(), pricing);
        }

        // No gateway leg at all (pure wallet funding) — nothing left to confirm.
        payment.setStatus(PaymentTxnStatus.PAID);
        payment.setProviderOrderId("wallet_only_" + payment.getId());
        paymentRepository.save(payment);
        applySettlementSideEffects(payment);
        return new PaymentInitiationResponse(payment.getId(), payment.getStatus().name(), null, null, pricing);
    }

    @Override
    @Transactional
    public PaymentStatusResponse verifyPayment(UUID userId, UUID paymentId, VerifyPaymentRequest request) {
        rateLimiter.checkAndConsume("payment-verify:" + userId,
                rateLimitProperties.paymentVerifyAttemptsPerWindow(),
                Duration.ofMinutes(rateLimitProperties.windowMinutes()),
                "Too many verification attempts. Please wait before trying again.");

        Payment payment = paymentRepository.lockByProviderOrderId(request.providerOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        // Same 404 whether the payment doesn't exist or just isn't this caller's — a 403
        // here would confirm someone else's provider order id is real.
        if (!payment.getUser().getId().equals(userId) || !payment.getId().equals(paymentId)) {
            throw new ResourceNotFoundException("Payment not found");
        }
        if (payment.getStatus() == PaymentTxnStatus.PAID) {
            return toStatusResponse(payment); // already settled — idempotent no-op
        }
        if (payment.getStatus() != PaymentTxnStatus.PENDING) {
            throw new BadRequestException("Payment is not awaiting verification");
        }

        PaymentProvider provider = paymentProviderFactory.resolve(payment.getProvider());
        ProviderVerificationResult result = provider.verifyPayment(new ProviderVerifyPaymentCommand(
                request.providerOrderId(), request.providerPaymentId(), request.signature()));
        if (!result.success()) {
            payment.setStatus(PaymentTxnStatus.FAILED);
            paymentRepository.save(payment);
            throw new BadRequestException("Payment verification failed: " + result.failureReason());
        }

        payment.setProviderPaymentId(request.providerPaymentId());
        payment.setSignature(request.signature());
        payment.setStatus(PaymentTxnStatus.PAID);
        paymentRepository.save(payment);
        applySettlementSideEffects(payment);
        return toStatusResponse(payment);
    }

    @Override
    @Transactional
    public PaymentStatusResponse mockComplete(UUID userId, UUID paymentId) {
        if (!paymentProperties.allowMockTopup()) {
            throw new BadRequestException(
                    "Mock payment completion is disabled; complete the payment through the gateway");
        }
        // Scalar projection, not a full entity fetch — see its javadoc for why: this
        // delegates (self-invoked, same bean) into verifyPayment's own locked lookup right
        // below, and pre-loading the entity here would poison that lock the same way an
        // earlier version of WalletServiceImpl's equivalent flow once did.
        String providerOrderId = paymentRepository.findProviderOrderIdByIdAndUserId(paymentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        String syntheticPaymentId = "pay_" + UUID.randomUUID().toString().replace("-", "");
        String signature = HmacSignatureVerifier.hmacSha256Hex(
                providerOrderId + "|" + syntheticPaymentId, paymentProperties.keySecret());

        return verifyPayment(userId, paymentId,
                new VerifyPaymentRequest(providerOrderId, syntheticPaymentId, signature));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(UUID userId, UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .filter(p -> p.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        return toStatusResponse(payment);
    }

    @Override
    @Transactional
    public RefundResponse refundPayment(UUID paymentId, RefundRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (payment.getStatus() != PaymentTxnStatus.PAID && payment.getStatus() != PaymentTxnStatus.PARTIALLY_REFUNDED) {
            throw new BadRequestException("Only a paid payment can be refunded");
        }

        List<Refund> existingRefunds = refundRepository.findByPayment_IdOrderByCreatedAtDesc(paymentId);
        BigDecimal alreadyRefunded = existingRefunds.stream()
                .map(Refund::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refundable = payment.getGrandTotal().subtract(alreadyRefunded);
        BigDecimal amount = request.amount() != null ? request.amount() : refundable;
        if (amount.signum() <= 0 || amount.compareTo(refundable) > 0) {
            throw new BadRequestException("Refund amount must be between 0 and " + refundable);
        }

        // Split proportionally to how the payment itself was funded. The platform fee
        // portion is excluded from both sides on purpose — it is never refunded to the
        // wallet or clawed back through the gateway, it stays platform revenue, mirroring
        // the same rule at collection time.
        BigDecimal refundableExcludingFee = payment.getGrandTotal().subtract(payment.getPlatformFee());
        BigDecimal walletShare = refundableExcludingFee.signum() == 0
                ? BigDecimal.ZERO
                : amount.multiply(payment.getWalletUsed())
                        .divide(refundableExcludingFee, 2, RoundingMode.HALF_UP)
                        .min(payment.getWalletUsed());
        BigDecimal gatewayShare = amount.subtract(walletShare);

        if (walletShare.signum() > 0) {
            walletService.credit(payment.getUser().getId(), walletShare, "REFUND", payment.getId().toString(),
                    "Refund for payment " + payment.getId());
        }
        String providerRefundId = null;
        if (gatewayShare.signum() > 0) {
            ProviderRefundResult result = paymentProviderFactory.resolve(payment.getProvider())
                    .refund(new ProviderRefundCommand(payment.getProviderPaymentId(), toPaise(gatewayShare), request.reason()));
            if (!result.success()) {
                throw new BadRequestException("Gateway refund failed: " + result.failureReason());
            }
            providerRefundId = result.providerRefundId();
        }

        Refund refund = new Refund();
        refund.setPayment(payment);
        refund.setWalletAmount(walletShare);
        refund.setGatewayAmount(gatewayShare);
        refund.setTotalAmount(amount);
        refund.setReason(request.reason());
        refund.setStatus(RefundStatus.PROCESSED);
        refund.setProviderRefundId(providerRefundId);
        refund = refundRepository.save(refund);

        BigDecimal totalRefundedNow = alreadyRefunded.add(amount);
        payment.setStatus(totalRefundedNow.compareTo(payment.getGrandTotal()) >= 0
                ? PaymentTxnStatus.REFUNDED : PaymentTxnStatus.PARTIALLY_REFUNDED);
        paymentRepository.save(payment);

        notificationService.notifyUser(payment.getUser(), NotificationEvent.REFUND_ISSUED,
                "Refund issued", amount + " has been refunded for your payment.",
                Map.of("paymentId", payment.getId().toString(), "amount", amount.toPlainString()));

        return new RefundResponse(refund.getId(), walletShare, gatewayShare, amount, refund.getStatus().name());
    }

    @Override
    public PricingBreakdown calculatePricing(CalculatePricingRequest request) {
        return switch (request.useCase()) {
            case WALLET_RECHARGE -> pricingService.calculateWalletRechargePricing(request.amount());
            case CHECKOUT -> pricingService.calculateCheckoutPricing(request.amount(), BigDecimal.ZERO, BigDecimal.ZERO,
                    request.paymentMode() != null ? request.paymentMode() : PaymentMode.GATEWAY_ONLY,
                    request.walletAmountAvailable() != null ? request.walletAmountAvailable() : BigDecimal.ZERO);
            case SUBSCRIPTION -> throw new BadRequestException("Subscription payments are not supported yet");
        };
    }

    @Override
    @Transactional
    public void handleWebhook(PaymentProviderType providerType, String rawBody, String signatureHeader) {
        PaymentProvider provider = paymentProviderFactory.resolve(providerType);
        WebhookVerificationResult result = provider.verifyWebhookSignature(rawBody, signatureHeader);
        if (!result.valid()) {
            log.warn("Rejected {} webhook with an invalid signature", providerType);
            throw new BadRequestException("Invalid webhook signature");
        }
        if (result.providerOrderId() == null) {
            log.info("Ignoring {} webhook event '{}' with no order reference", providerType, result.eventType());
            return;
        }
        Payment payment = paymentRepository.lockByProviderOrderId(result.providerOrderId()).orElse(null);
        if (payment == null) {
            log.warn("{} webhook referenced unknown provider order {}", providerType, result.providerOrderId());
            return;
        }
        if (payment.getStatus() != PaymentTxnStatus.PENDING) {
            return; // already settled (by a client verify call) or already failed — no-op either way
        }
        payment.setProviderPaymentId(result.providerPaymentId());
        payment.setStatus(PaymentTxnStatus.PAID);
        paymentRepository.save(payment);
        applySettlementSideEffects(payment);
    }

    /** Fires whichever use-case-specific effect turns a PAID payment into a real outcome:
     *  crediting the wallet for a recharge, or marking an order paid for a checkout. */
    private void applySettlementSideEffects(Payment payment) {
        switch (payment.getUseCase()) {
            case WALLET_RECHARGE -> {
                walletService.credit(payment.getUser().getId(), payment.getSubtotal(), "PAYMENT",
                        payment.getId().toString(), "Wallet recharge");
                notificationService.notifyUser(payment.getUser(), NotificationEvent.WALLET_TOPPED_UP,
                        "Wallet topped up", payment.getSubtotal() + " was added to your wallet.",
                        Map.of("amount", payment.getSubtotal().toPlainString()));
            }
            case CHECKOUT -> settleOrder(payment);
            case SUBSCRIPTION -> log.warn("Settled a SUBSCRIPTION payment {} — no handler exists yet", payment.getId());
        }
    }

    private void settleOrder(Payment payment) {
        if (!"ORDER".equals(payment.getReferenceType()) || payment.getReferenceId() == null) {
            log.warn("CHECKOUT payment {} has no order reference to settle", payment.getId());
            return;
        }
        UUID orderId;
        try {
            orderId = UUID.fromString(payment.getReferenceId());
        } catch (IllegalArgumentException ex) {
            log.warn("CHECKOUT payment {} has a malformed order reference '{}'", payment.getId(), payment.getReferenceId());
            return;
        }
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("CHECKOUT payment {} references missing order {}", payment.getId(), orderId);
            return;
        }
        if (order.getPaymentStatus() == PaymentStatus.PENDING) {
            order.setPaymentStatus(PaymentStatus.PAID);
            orderRepository.save(order);
            notificationService.notifyUser(order.getPlacedBy(), NotificationEvent.PAYMENT_SUCCESSFUL,
                    "Payment successful", "Paid " + payment.getGrandTotal() + " for order " + order.getOrderNumber() + ".",
                    Map.of("orderId", order.getId().toString(), "amount", payment.getGrandTotal().toPlainString()));
        }
    }

    private PaymentInitiationResponse toInitiationResponse(Payment payment) {
        PricingBreakdown pricing = new PricingBreakdown(payment.getSubtotal(), payment.getPlatformFee(),
                payment.getDiscount(), payment.getTax(), payment.getWalletUsed(), payment.getGatewayAmount(),
                payment.getGrandTotal(), payment.getCurrency());
        boolean hasGatewayLeg = payment.getGatewayAmount().signum() > 0;
        return new PaymentInitiationResponse(payment.getId(), payment.getStatus().name(),
                hasGatewayLeg ? payment.getProviderOrderId() : null,
                hasGatewayLeg ? paymentProperties.keyId() : null,
                pricing);
    }

    private PaymentStatusResponse toStatusResponse(Payment payment) {
        PricingBreakdown pricing = new PricingBreakdown(payment.getSubtotal(), payment.getPlatformFee(),
                payment.getDiscount(), payment.getTax(), payment.getWalletUsed(), payment.getGatewayAmount(),
                payment.getGrandTotal(), payment.getCurrency());
        return new PaymentStatusResponse(payment.getId(), payment.getUseCase(), payment.getStatus().name(),
                payment.getProvider().name(), payment.getProviderPaymentId(), pricing, payment.getCreatedAt());
    }

    private static long toPaise(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }

    private static String receiptFor(Payment payment) {
        return payment.getUseCase().name().toLowerCase() + "_" + payment.getId();
    }

    private static String describeWalletDebit(PaymentUseCase useCase) {
        return useCase == PaymentUseCase.CHECKOUT ? "Order payment (wallet portion)" : "Payment (wallet portion)";
    }
}
