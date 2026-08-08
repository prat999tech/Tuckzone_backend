package com.school.canteen.notification;

import com.school.canteen.config.NotificationProperties;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Real email delivery via Brevo's transactional HTTPS API.
 *
 * Exists alongside {@link SmtpEmailSender} for one reason: raw SMTP is blocked outbound on
 * Railway's Free/Trial/Hobby plans (Pro-only), so any deploy on those tiers needs an
 * HTTPS-based provider instead. Brevo was picked because it was already the SMTP provider
 * in use — this reuses the same account/verified sender, just swapping the transport.
 *
 * Active when {@code app.notification.email-provider=brevo}.
 */
@Component
@ConditionalOnProperty(name = "app.notification.email-provider", havingValue = "brevo")
public class BrevoEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(BrevoEmailSender.class);
    private static final String ENDPOINT = "https://api.brevo.com/v3/smtp/email";

    private final RestClient restClient;
    private final NotificationProperties properties;

    public BrevoEmailSender(NotificationProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    @PostConstruct
    void validateConfig() {
        // Fail at boot rather than on the first real email: a missing key/address would
        // otherwise silently fail every send until someone noticed users had stopped
        // receiving codes.
        if (properties.brevoApiKey() == null || properties.brevoApiKey().isBlank()) {
            throw new IllegalStateException(
                    "app.notification.email-provider=brevo requires BREVO_API_KEY to be set");
        }
        if (properties.emailFromAddress() == null || properties.emailFromAddress().isBlank()) {
            throw new IllegalStateException(
                    "app.notification.email-provider=brevo requires EMAIL_FROM_ADDRESS to be set");
        }
    }

    @Override
    public boolean send(EmailMessage message) {
        try {
            Map<String, Object> body = Map.of(
                    "sender", Map.of("name", properties.emailFromName(), "email", properties.emailFromAddress()),
                    "to", List.of(Map.of("email", message.to())),
                    "subject", message.subject(),
                    "htmlContent", message.htmlBody(),
                    "textContent", message.textBody());

            restClient.post()
                    .uri(ENDPOINT)
                    .header("api-key", properties.brevoApiKey())
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception ex) {
            // Never propagate: callers mark the outbox row FAILED and the sweeper retries,
            // so a provider hiccup must not surface as an error on an unrelated request.
            log.warn("Brevo send failed for {}", message.to(), ex);
            return false;
        }
    }
}
