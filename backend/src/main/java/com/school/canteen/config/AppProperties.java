package com.school.canteen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Top-level application settings.
 *
 * @param timezone the school's civil timezone. Every "what is today" and "is it past the
 *                 order cutoff" decision resolves against this zone rather than the
 *                 server's default, because deploy hosts run in UTC.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String timezone,
        /** Oldest mobile client build still supported; older installs are asked to update. */
        String minimumAppVersion) {
}
