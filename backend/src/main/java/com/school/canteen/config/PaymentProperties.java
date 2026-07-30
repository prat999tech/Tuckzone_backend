package com.school.canteen.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Payment gateway settings.
 *
 * @param allowMockTopup when true, {@code /api/wallet/topup/mock-complete} credits a
 *                       wallet without real money. This exists because there is no real
 *                       payment gateway yet; it is an explicit, single switch rather than
 *                       something inherited by accident, so it can be turned off in one
 *                       step once a real gateway is integrated.
 * @param maxTopupAmount hard ceiling for a single top-up, capping abuse while mock mode is on.
 */
@ConfigurationProperties(prefix = "app.payment")
public record PaymentProperties(
        String provider,
        String currency,
        String keyId,
        String keySecret,
        boolean allowMockTopup,
        BigDecimal maxTopupAmount) {
}
