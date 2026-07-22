package com.school.canteen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payment")
public record PaymentProperties(
        String provider,
        String currency,
        String keyId,
        String keySecret) {
}
