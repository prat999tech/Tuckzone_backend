package com.school.canteen.payment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * HMAC-SHA256 signing/verification shared by every provider that uses this scheme
 * (Razorpay's checkout signature is {@code HMAC_SHA256(order_id + "|" + payment_id,
 * key_secret)}; its webhook signature is {@code HMAC_SHA256(raw_body, webhook_secret)} —
 * same primitive, different input). Kept out of any one provider class so the logic is
 * written and tested once, not duplicated per gateway.
 */
public final class HmacSignatureVerifier {

    private static final String ALGORITHM = "HmacSHA256";

    private HmacSignatureVerifier() {
    }

    public static String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to compute HMAC signature", ex);
        }
    }

    /** Constant-time comparison so verification time can't leak the correct signature. */
    public static boolean matches(String data, String secret, String suppliedSignature) {
        if (data == null || secret == null || suppliedSignature == null) {
            return false;
        }
        String expected = hmacSha256Hex(data, secret);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                suppliedSignature.getBytes(StandardCharsets.UTF_8));
    }
}
