package com.school.canteen.notification;

import com.school.canteen.config.NotificationProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.SesClientBuilder;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SesException;

/**
 * Real email delivery via the Amazon SES HTTPS API.
 *
 * Credentials and region are never read manually: {@code SesClient.builder()} resolves
 * them from the SDK's default provider chains, which check (in order) the
 * AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY / AWS_REGION environment variables, then
 * {@code ~/.aws/credentials} and {@code ~/.aws/config}, then an IAM role attached to the
 * host. Nothing AWS-specific is hand-parsed here, so the same code runs unchanged on a
 * developer laptop (env vars) and in production on ECS/EC2 (an attached IAM role — no
 * long-lived secret on disk at all).
 *
 * Active when {@code app.notification.email-provider=ses}.
 */
@Component
@ConditionalOnProperty(name = "app.notification.email-provider", havingValue = "ses")
public class SesEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SesEmailSender.class);
    private static final String CHARSET = "UTF-8";

    private final NotificationProperties properties;
    private final SesClient sesClient;

    public SesEmailSender(NotificationProperties properties) {
        this.properties = properties;
        SesClientBuilder builder = SesClient.builder();
        // Explicit region is optional — leaving it unset lets the SDK's own region chain
        // (AWS_REGION env var, ~/.aws/config) decide, same as credentials above.
        if (properties.sesRegion() != null && !properties.sesRegion().isBlank()) {
            builder.region(Region.of(properties.sesRegion()));
        }
        this.sesClient = builder.build();
    }

    @PostConstruct
    void validateConfig() {
        // Fail at boot rather than on the first real email: an unverified or missing From
        // address would otherwise silently fail every send until someone noticed users had
        // stopped receiving codes.
        if (properties.emailFromAddress() == null || properties.emailFromAddress().isBlank()) {
            throw new IllegalStateException(
                    "app.notification.email-provider=ses requires EMAIL_FROM_ADDRESS to be set "
                            + "to a verified SES sender identity (a verified address, or any "
                            + "address on a verified domain)");
        }
    }

    @Override
    public boolean send(EmailMessage message) {
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .source(sourceHeader())
                    .destination(Destination.builder().toAddresses(message.to()).build())
                    .message(Message.builder()
                            .subject(Content.builder().charset(CHARSET).data(message.subject()).build())
                            .body(Body.builder()
                                    .html(Content.builder().charset(CHARSET).data(message.htmlBody()).build())
                                    .text(Content.builder().charset(CHARSET).data(message.textBody()).build())
                                    .build())
                            .build())
                    .build();
            sesClient.sendEmail(request);
            return true;
        } catch (SesException ex) {
            // Never propagate: callers mark the outbox row FAILED and the sweeper retries,
            // so an SES-side rejection (throttled, unverified identity, sandbox
            // restriction) must not surface as an error on an unrelated request.
            String detail = ex.awsErrorDetails() != null
                    ? ex.awsErrorDetails().errorMessage() : ex.getMessage();
            log.warn("SES send failed for {}: {}", message.to(), detail);
            return false;
        } catch (Exception ex) {
            log.warn("SES send failed for {}", message.to(), ex);
            return false;
        }
    }

    /** "Display Name <address>" — SES accepts the same RFC 5322 form SMTP does. */
    private String sourceHeader() {
        String name = properties.emailFromName();
        String address = properties.emailFromAddress();
        return (name == null || name.isBlank()) ? address : name + " <" + address + ">";
    }

    @PreDestroy
    void shutdown() {
        sesClient.close();
    }
}
