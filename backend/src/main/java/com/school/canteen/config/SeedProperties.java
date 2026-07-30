package com.school.canteen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Optional bootstrap account for a brand-new environment.
 *
 * Disabled by default: admins register through the normal sign-up flow using the invite
 * code. This exists only so a fresh deployment is not locked out before anyone has
 * registered.
 */
@ConfigurationProperties(prefix = "app.seed")
public record SeedProperties(
        boolean enabled,
        Account canteenAdmin) {

    public record Account(
            String fullName,
            String email,
            String mobile,
            String password) {
    }
}
