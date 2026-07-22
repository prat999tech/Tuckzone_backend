package com.school.canteen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binding for app.seed.* — the bootstrap admin accounts created on startup.
 */
@ConfigurationProperties(prefix = "app.seed")
public record SeedProperties(
        boolean enabled,
        Account schoolAdmin,
        Account canteenAdmin) {

    public record Account(
            String fullName,
            String email,
            String mobile,
            String password) {
    }
}
