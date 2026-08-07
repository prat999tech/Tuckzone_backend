package com.school.canteen.payment.providers;

import com.school.canteen.config.PaymentProperties;
import com.school.canteen.payment.HmacSignatureVerifier;
import com.school.canteen.payment.PaymentProvider;
import com.school.canteen.payment.PaymentProviderType;
import com.school.canteen.payment.ProviderCreateOrderCommand;
import com.school.canteen.payment.ProviderOrder;
import com.school.canteen.payment.ProviderRefundCommand;
import com.school.canteen.payment.ProviderRefundResult;
import com.school.canteen.payment.ProviderVerificationResult;
import com.school.canteen.payment.ProviderVerifyPaymentCommand;
import com.school.canteen.payment.WebhookVerificationResult;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * In-process provider for development/testing. Mirrors Razorpay's real signature scheme
 * exactly ({@code HMAC_SHA256(order_id + "|" + payment_id, key_secret)}), so a test can
 * produce a valid signature the same way a real callback would, and this is a genuine
 * drop-in swap for {@link RazorpayProvider} rather than a special case elsewhere.
 *
 * Selected by {@code PaymentProviderFactory} when {@code app.payment.provider=mock}
 * (the default) — this class itself is always registered, unconditionally, since
 * selection is the factory's job, not Spring's bean-conditional machinery.
 */
@Component
public class MockPaymentProvider implements PaymentProvider {

    private final PaymentProperties properties;

    public MockPaymentProvider(PaymentProperties properties) {
        this.properties = properties;
    }

    @Override
    public PaymentProviderType type() {
        return PaymentProviderType.MOCK;
    }

    @Override
    public ProviderOrder createOrder(ProviderCreateOrderCommand command) {
        String orderId = "order_" + UUID.randomUUID().toString().replace("-", "");
        return new ProviderOrder(orderId, command.amountPaise(), command.currency(), properties.keyId());
    }

    @Override
    public ProviderVerificationResult verifyPayment(ProviderVerifyPaymentCommand command) {
        String data = command.providerOrderId() + "|" + command.providerPaymentId();
        boolean genuine = HmacSignatureVerifier.matches(data, properties.keySecret(), command.signature());
        return genuine
                ? ProviderVerificationResult.ok()
                : ProviderVerificationResult.failure("Signature mismatch");
    }

    @Override
    public ProviderRefundResult refund(ProviderRefundCommand command) {
        // No real money ever moved, so there is nothing to reverse with a gateway — a mock
        // refund always "succeeds" immediately with a synthetic id.
        return ProviderRefundResult.success("rfnd_mock_" + UUID.randomUUID().toString().replace("-", ""));
    }

    @Override
    public WebhookVerificationResult verifyWebhookSignature(String rawBody, String signatureHeader) {
        // The mock provider never sends webhooks — nothing calls this in practice, but
        // implementing it for real keeps the mock a faithful stand-in for a real provider.
        return WebhookVerificationResult.invalid();
    }
}
