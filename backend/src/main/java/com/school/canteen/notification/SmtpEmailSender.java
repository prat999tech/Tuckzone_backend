package com.school.canteen.notification;

import com.school.canteen.config.NotificationProperties;
import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Real email delivery over plain SMTP.
 *
 * SMTP is chosen deliberately over a vendor SDK (SendGrid, Resend, Mailgun...): it is a
 * standard protocol every mail provider speaks, so moving between Gmail, Zoho, or a
 * self-hosted server is a matter of changing host/port/username/password — no dependency
 * swap, no code change, no lock-in.
 *
 * Connection settings come from Spring Boot's standard {@code spring.mail.*} properties.
 * Active when {@code app.notification.email-provider=smtp}.
 */
@Component
@ConditionalOnProperty(name = "app.notification.email-provider", havingValue = "smtp")
public class SmtpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    public SmtpEmailSender(JavaMailSender mailSender, NotificationProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @PostConstruct
    void validateConfig() {
        // Fail at boot rather than on the first real email: a missing From address would
        // otherwise silently fail every send until someone noticed users had stopped
        // receiving codes.
        if (properties.emailFromAddress() == null || properties.emailFromAddress().isBlank()) {
            throw new IllegalStateException(
                    "app.notification.email-provider=smtp requires EMAIL_FROM_ADDRESS to be set");
        }
    }

    @Override
    public boolean send(EmailMessage message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            // multipart=true is required for the two-argument setText(plain, html) below —
            // it produces a multipart/alternative message so mail clients that cannot (or
            // choose not to) render HTML still show something readable.
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.textBody(), message.htmlBody());
            setFrom(helper);
            mailSender.send(mimeMessage);
            return true;
        } catch (Exception ex) {
            // Never propagate: callers mark the outbox row FAILED and the sweeper retries,
            // so a mail-server hiccup must not surface as an error on an unrelated request.
            log.warn("SMTP send failed for {}", message.to(), ex);
            return false;
        }
    }

    private void setFrom(MimeMessageHelper helper) throws Exception {
        try {
            helper.setFrom(properties.emailFromAddress(), properties.emailFromName());
        } catch (UnsupportedEncodingException ex) {
            // Display name could not be encoded — fall back to the bare address rather
            // than failing the whole send over cosmetics.
            helper.setFrom(properties.emailFromAddress());
        }
    }
}
