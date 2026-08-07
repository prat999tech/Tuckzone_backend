package com.school.canteen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The one setting that's genuinely Razorpay-specific rather than generic-provider (key id
 * and secret live on {@link PaymentProperties} since every provider needs some key pair).
 *
 * @param webhookSecret shared secret configured in the Razorpay dashboard's webhook
 *                       settings; verifies inbound webhook calls are genuinely from
 *                       Razorpay, separate from the checkout signature secret.
 */
@ConfigurationProperties(prefix = "app.payment.razorpay")
public record RazorpayProperties(String webhookSecret) {
}
