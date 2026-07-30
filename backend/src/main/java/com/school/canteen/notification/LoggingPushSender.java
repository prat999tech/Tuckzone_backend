package com.school.canteen.notification;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Development push sender: writes each notification to the application log and reports
 * success, so the whole outbox pipeline is exercisable without a Firebase project.
 *
 * Active while {@code app.notification.push-provider=log}.
 */
@Component
@ConditionalOnProperty(name = "app.notification.push-provider", havingValue = "log",
        matchIfMissing = true)
public class LoggingPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingPushSender.class);

    @Override
    public List<PushOutcome> sendAll(List<PushMessage> messages) {
        for (PushMessage message : messages) {
            log.info("[DEV PUSH] to={} | {} | {} | data={}",
                    abbreviate(message.token()), message.title(), message.body(), message.data());
        }
        return messages.stream().map(ignored -> PushOutcome.ok()).toList();
    }

    @Override
    public int maxBatchSize() {
        return 500;
    }

    private static String abbreviate(String token) {
        return (token != null && token.length() > 12) ? token.substring(0, 12) + "..." : token;
    }
}
