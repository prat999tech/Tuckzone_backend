package com.school.canteen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Throttling limits for credential and OTP endpoints, expressed as "N attempts per window".
 */
@ConfigurationProperties(prefix = "app.security.rate-limit")
public record RateLimitProperties(
        int loginAttemptsPerWindow,
        int otpRequestsPerWindow,
        int otpVerifyAttemptsPerWindow,
        int windowMinutes) {
}
