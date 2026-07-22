package com.school.canteen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe binding of the app.security.jwt.* config. A record because these values are
 * read-only configuration. Relaxed binding maps kebab-case yaml keys
 * (access-token-ttl-minutes) to camelCase record components.
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String secret,
        long accessTokenTtlMinutes,
        long refreshTokenTtlDays) {
}
