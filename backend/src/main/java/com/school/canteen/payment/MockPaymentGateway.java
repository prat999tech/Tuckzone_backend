package com.school.canteen.payment;

import com.school.canteen.config.PaymentProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * In-process gateway for development/testing. It mirrors Razorpay's real scheme exactly:
 * the expected signature is HMAC_SHA256(orderId + "|" + paymentId, keySecret) in hex. That
 * means a test (or the real Razorpay callback) can produce a valid signature the same way,
 * and swapping in a real Razorpay implementation changes nothing else.
 *
 * Active by default; a real provider bean would use havingValue = "razorpay".
 */
@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentGateway implements PaymentGateway {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final PaymentProperties properties;

    public MockPaymentGateway(PaymentProperties properties) {
        this.properties = properties;
    }

    @Override
    public GatewayOrder createOrder(long amountPaise, String receipt) {
        String orderId = "order_" + UUID.randomUUID().toString().replace("-", "");
        return new GatewayOrder(orderId, amountPaise, properties.currency(), properties.keyId());
    }

    @Override
    public boolean verifyPayment(String orderId, String paymentId, String signature) {
        if (orderId == null || paymentId == null || signature == null) {
            return false;
        }
        String expected = hmacSha256Hex(orderId + "|" + paymentId, properties.keySecret());
        // Constant-time comparison so verification time can't leak the correct signature.
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    private static String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to compute HMAC signature", ex);
        }
    }
}
