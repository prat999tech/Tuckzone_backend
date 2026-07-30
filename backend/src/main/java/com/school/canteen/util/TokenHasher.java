package com.school.canteen.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * One-way hashing for opaque tokens stored server-side.
 *
 * Refresh tokens are kept as SHA-256 hashes so that a database leak does not hand an
 * attacker usable sessions. SHA-256 (not BCrypt) is right here because the input is
 * already high-entropy random data, and lookups must be fast and by exact value.
 */
public final class TokenHasher {

    private TokenHasher() {
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required but unavailable", ex);
        }
    }
}
