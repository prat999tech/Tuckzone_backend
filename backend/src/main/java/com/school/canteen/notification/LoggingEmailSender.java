package com.school.canteen.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Development email sender: writes the message to the application log instead of
 * delivering it, so the outbox pipeline is exercisable without an email provider account.
 *
 * Active while {@code app.notification.email-provider=log} (the default).
 */
@Component
@ConditionalOnProperty(name = "app.notification.email-provider", havingValue = "log",
        matchIfMissing = true)
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public boolean send(EmailMessage message) {
        log.info("[DEV EMAIL] to={} | subject={} | body={}",
                message.to(), message.subject(), message.htmlBody());
        return true;
    }
}
