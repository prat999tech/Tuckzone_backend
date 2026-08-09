package com.school.canteen.payment.providers;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;
import com.school.canteen.config.PaymentProperties;
import com.school.canteen.config.RazorpayProperties;
import com.school.canteen.payment.HmacSignatureVerifier;
import com.school.canteen.payment.PaymentProvider;
import com.school.canteen.payment.PaymentProviderType;
import com.school.canteen.payment.ProviderCreateOrderCommand;
import com.school.canteen.payment.ProviderOrder;
import com.school.canteen.payment.ProviderRefundCommand;
import com.school.canteen.payment.ProviderRefundResult;
import com.school.canteen.payment.ProviderVerificationResult;
import com.school.canteen.payment.ProviderVerifyPaymentCommand;
import com.school.canteen.payment.WebhookOutcome;
import com.school.canteen.payment.WebhookVerificationResult;
import jakarta.annotation.PostConstruct;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Real payment delivery via Razorpay. This is the ONLY file in the codebase that imports
 * the Razorpay SDK — everything else talks to {@link PaymentProvider}.
 *
 * Signature verification deliberately does NOT call {@code Utils.verifyPaymentSignature}
 * from the SDK: Razorpay's own source confirms that method is exactly
 * {@code HMAC_SHA256(order_id + "|" + payment_id, key_secret)} hex-compared, which is the
 * same primitive {@link HmacSignatureVerifier} already implements and
 * {@code MockPaymentProvider} already exercises — reusing it here means the
 * security-critical path is one audited implementation, not two. The SDK IS used for the
 * network calls that can't be faked locally: creating an order and issuing a refund.
 *
 * Active when {@code app.payment.provider=razorpay}. Not yet exercised against a live
 * Razorpay sandbox — verify with real test-mode keys before relying on this in production
 * (see docs/payments/TESTING_GUIDE.md).
 */
@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "razorpay")
public class RazorpayProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(RazorpayProvider.class);

    private final PaymentProperties paymentProperties;
    private final RazorpayProperties razorpayProperties;
    private RazorpayClient client;

    public RazorpayProvider(PaymentProperties paymentProperties, RazorpayProperties razorpayProperties) {
        this.paymentProperties = paymentProperties;
        this.razorpayProperties = razorpayProperties;
    }

    @PostConstruct
    void init() {
        // Fail at boot rather than on the first real checkout — a missing key would
        // otherwise silently break every payment until someone noticed.
        if (isBlank(paymentProperties.keyId()) || isBlank(paymentProperties.keySecret())) {
            throw new IllegalStateException(
                    "app.payment.provider=razorpay requires RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET");
        }
        try {
            this.client = new RazorpayClient(paymentProperties.keyId(), paymentProperties.keySecret());
        } catch (RazorpayException ex) {
            throw new IllegalStateException("Could not initialise the Razorpay client", ex);
        }
    }

    @Override
    public PaymentProviderType type() {
        return PaymentProviderType.RAZORPAY;
    }

    @Override
    public ProviderOrder createOrder(ProviderCreateOrderCommand command) {
        try {
            JSONObject request = new JSONObject();
            request.put("amount", command.amountPaise());
            request.put("currency", command.currency());
            request.put("receipt", command.receipt());
            if (command.notes() != null && !command.notes().isEmpty()) {
                request.put("notes", new JSONObject(command.notes()));
            }
            Order order = client.orders.create(request);
            String orderId = order.get("id");
            return new ProviderOrder(orderId, command.amountPaise(), command.currency(), paymentProperties.keyId());
        } catch (RazorpayException ex) {
            log.error("Razorpay order creation failed for receipt {}", command.receipt(), ex);
            throw new IllegalStateException("Could not create Razorpay order", ex);
        }
    }

    @Override
    public ProviderVerificationResult verifyPayment(ProviderVerifyPaymentCommand command) {
        String data = command.providerOrderId() + "|" + command.providerPaymentId();
        boolean genuine = HmacSignatureVerifier.matches(data, paymentProperties.keySecret(), command.signature());
        return genuine
                ? ProviderVerificationResult.ok()
                : ProviderVerificationResult.failure("Signature mismatch");
    }

    @Override
    public ProviderRefundResult refund(ProviderRefundCommand command) {
        try {
            JSONObject request = new JSONObject();
            request.put("amount", command.amountPaise());
            if (command.reason() != null) {
                request.put("notes", new JSONObject().put("reason", command.reason()));
            }
            Refund refund = client.payments.refund(command.providerPaymentId(), request);
            String refundId = refund.get("id");
            return ProviderRefundResult.success(refundId);
        } catch (RazorpayException ex) {
            log.error("Razorpay refund failed for payment {}", command.providerPaymentId(), ex);
            return ProviderRefundResult.failure(ex.getMessage());
        }
    }

    @Override
    public WebhookVerificationResult verifyWebhookSignature(String rawBody, String signatureHeader) {
        if (isBlank(razorpayProperties.webhookSecret())) {
            log.warn("Received a Razorpay webhook but no webhook secret is configured; rejecting");
            return WebhookVerificationResult.invalid();
        }
        if (!HmacSignatureVerifier.matches(rawBody, razorpayProperties.webhookSecret(), signatureHeader)) {
            return WebhookVerificationResult.invalid();
        }
        try {
            JSONObject payload = new JSONObject(rawBody);
            String eventType = payload.optString("event", null);
            JSONObject paymentEntity = payload
                    .optJSONObject("payload")
                    .optJSONObject("payment")
                    .optJSONObject("entity");
            String providerPaymentId = paymentEntity != null ? paymentEntity.optString("id", null) : null;
            String providerOrderId = paymentEntity != null ? paymentEntity.optString("order_id", null) : null;
            return new WebhookVerificationResult(true, outcomeFor(eventType), providerOrderId, providerPaymentId);
        } catch (Exception ex) {
            log.warn("Razorpay webhook signature was valid but the payload could not be parsed", ex);
            return WebhookVerificationResult.invalid();
        }
    }

    /**
     * Razorpay's own event catalogue, narrowed to the two outcomes {@code PaymentService}
     * acts on. {@code payment.captured} is the only event treated as success — deliberately
     * NOT {@code order.paid}, which fires from the order's own payment entity payload
     * (different shape) and NOT {@code payment.authorized}, which (outside auto-capture)
     * means money is only held, not actually captured yet. {@code payment.failed} is the
     * gateway explicitly telling us an attempt did not go through — e.g. a customer who
     * picked UPI, got as far as an intent/collect request, then backed out or had it
     * declined. Everything else (refund events, disputes, etc.) is ignored here; this
     * webhook only ever settles or voids a checkout/recharge payment.
     */
    private static WebhookOutcome outcomeFor(String eventType) {
        if (eventType == null) {
            return WebhookOutcome.IGNORE;
        }
        return switch (eventType) {
            case "payment.captured" -> WebhookOutcome.SUCCESS;
            case "payment.failed" -> WebhookOutcome.FAILURE;
            default -> WebhookOutcome.IGNORE;
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
