package com.school.canteen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * One-time passcode settings.
 *
 * Codes are delivered by email. SMS was deliberately dropped: it requires DLT registration
 * in India (business entity, sender id and exact template wording pre-approved by a
 * telecom aggregator) plus a paid per-message gateway, whereas email needs only an SMTP
 * account and works with any provider.
 *
 * @param delivery     which {@link com.school.canteen.notification.OtpSender} is active:
 *                     "log" for development, "email" for real delivery
 * @param length       number of digits in a generated code
 * @param ttlMinutes   how long a code stays valid
 * @param maxAttempts  wrong guesses allowed before the code is burned
 * @param resendCooldownSeconds minimum gap enforced between two codes issued to the same
 *                              address for the same purpose. Distinct from the sliding-
 *                              window rate limit in {@link RateLimitProperties}: that caps
 *                              total volume over 15 minutes, this stops back-to-back taps
 *                              on "Resend" from burning through that budget in seconds.
 */
@ConfigurationProperties(prefix = "app.otp")
public record OtpProperties(
        String delivery,
        int length,
        int ttlMinutes,
        int maxAttempts,
        int resendCooldownSeconds) {
}
