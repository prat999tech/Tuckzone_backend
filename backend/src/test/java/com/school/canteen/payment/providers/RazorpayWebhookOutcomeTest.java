package com.school.canteen.payment.providers;

import static org.assertj.core.api.Assertions.assertThat;

import com.school.canteen.config.PaymentProperties;
import com.school.canteen.config.RazorpayProperties;
import com.school.canteen.payment.HmacSignatureVerifier;
import com.school.canteen.payment.WebhookOutcome;
import com.school.canteen.payment.WebhookVerificationResult;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pure unit test, no Spring context: pins down that a Razorpay webhook is classified by its
 * actual event type rather than treated as an unconditional success. Before this, any
 * validly-signed webhook that referenced a payment — including {@code payment.failed}, sent
 * when a customer's checkout attempt is declined or abandoned partway through — was marked
 * PAID and settled the order. This is the other half of "cancel payment, order still
 * placed": a real Razorpay account can fire {@code payment.failed} for exactly that
 * scenario, and the old code would have confirmed the order anyway.
 */
class RazorpayWebhookOutcomeTest {

    private static final String WEBHOOK_SECRET = "test-webhook-secret";

    private RazorpayProvider provider;

    @BeforeEach
    void setUp() {
        PaymentProperties paymentProperties = new PaymentProperties(
                "razorpay", "INR", "rzp_test_key", "rzp_test_secret", false, BigDecimal.valueOf(10000));
        RazorpayProperties razorpayProperties = new RazorpayProperties(WEBHOOK_SECRET);
        provider = new RazorpayProvider(paymentProperties, razorpayProperties);
        provider.init();
    }

    private String sign(String rawBody) {
        return HmacSignatureVerifier.hmacSha256Hex(rawBody, WEBHOOK_SECRET);
    }

    private String payloadFor(String event) {
        return """
                {"event":"%s","payload":{"payment":{"entity":{"id":"pay_abc123","order_id":"order_xyz789"}}}}
                """.formatted(event).strip();
    }

    @Test
    void paymentCapturedIsSuccess() {
        String rawBody = payloadFor("payment.captured");
        WebhookVerificationResult result = provider.verifyWebhookSignature(rawBody, sign(rawBody));

        assertThat(result.valid()).isTrue();
        assertThat(result.outcome()).isEqualTo(WebhookOutcome.SUCCESS);
        assertThat(result.providerOrderId()).isEqualTo("order_xyz789");
        assertThat(result.providerPaymentId()).isEqualTo("pay_abc123");
    }

    @Test
    void paymentFailedIsFailureNotSuccess() {
        String rawBody = payloadFor("payment.failed");
        WebhookVerificationResult result = provider.verifyWebhookSignature(rawBody, sign(rawBody));

        assertThat(result.valid()).isTrue();
        assertThat(result.outcome()).isEqualTo(WebhookOutcome.FAILURE);
        assertThat(result.providerOrderId()).isEqualTo("order_xyz789");
    }

    @Test
    void paymentAuthorizedIsIgnoredNotTreatedAsSuccess() {
        // Authorized-but-not-captured (relevant when auto-capture is off) must not settle
        // the order — only a genuine capture should.
        String rawBody = payloadFor("payment.authorized");
        WebhookVerificationResult result = provider.verifyWebhookSignature(rawBody, sign(rawBody));

        assertThat(result.valid()).isTrue();
        assertThat(result.outcome()).isEqualTo(WebhookOutcome.IGNORE);
    }

    @Test
    void unrecognisedEventIsIgnored() {
        String rawBody = payloadFor("refund.processed");
        WebhookVerificationResult result = provider.verifyWebhookSignature(rawBody, sign(rawBody));

        assertThat(result.valid()).isTrue();
        assertThat(result.outcome()).isEqualTo(WebhookOutcome.IGNORE);
    }

    @Test
    void invalidSignatureIsRejectedRegardlessOfOutcome() {
        String rawBody = payloadFor("payment.captured");
        WebhookVerificationResult result = provider.verifyWebhookSignature(rawBody, "not-the-real-signature");

        assertThat(result.valid()).isFalse();
    }
}
