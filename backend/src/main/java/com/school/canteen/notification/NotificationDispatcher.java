package com.school.canteen.notification;

import com.school.canteen.config.AsyncConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * The fast path: sends a notification the instant its transaction commits.
 *
 * AFTER_COMMIT guarantees the order (or payment, or status change) is durably saved before
 * anyone is told about it. {@code @Async} then moves the provider call off the request
 * thread, so the customer's "Place Order" response is never held up waiting for FCM.
 *
 * If this fails or is rejected by a saturated pool, the row simply stays PENDING and
 * {@link OutboxSweeper} delivers it moments later — the notification is never lost.
 */
@Component
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final NotificationDeliveryService deliveryService;

    public NotificationDispatcher(NotificationDeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationsQueued(NotificationsQueuedEvent event) {
        try {
            deliveryService.deliver(event.outboxIds());
        } catch (RuntimeException ex) {
            // Swallow deliberately: the sweeper owns the retry, and an exception thrown
            // from an async listener would otherwise vanish into the pool's handler.
            log.warn("Immediate notification delivery failed; leaving it for the sweeper", ex);
        }
    }
}
