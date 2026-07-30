package com.school.canteen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Admin onboarding settings.
 *
 * @param signupCode shared secret required to register a CANTEEN_ADMIN account. Without
 *                   it, anyone who can reach the public registration endpoint could make
 *                   themselves the canteen owner.
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(String signupCode) {
}
