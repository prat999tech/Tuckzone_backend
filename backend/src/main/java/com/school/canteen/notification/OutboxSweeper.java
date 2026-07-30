package com.school.canteen.notification;

import com.school.canteen.config.NotificationProperties;
import com.school.canteen.repository.NotificationOutboxRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The safety net.
 *
 * In normal operation this finds nothing to do: notifications go out within milliseconds
 * of commit via {@link NotificationDispatcher}. It exists for the cases the fast path
 * cannot cover — the provider was down, the thread pool was saturated by a rush of orders,
 * or the application was restarted mid-send.
 *
 * Running every two seconds keeps the worst-case delay small while costing only an indexed
 * lookup on a table that is almost always empty of due rows.
 */
@Component
public class OutboxSweeper {

    private static final Logger log = LoggerFactory.getLogger(OutboxSweeper.class);

    /** A row held in PROCESSING longer than this belonged to a run that died. */
    private static final Duration STUCK_AFTER = Duration.ofMinutes(2);

    private final NotificationOutboxRepository outboxRepository;
    private final NotificationDeliveryService deliveryService;
    private final NotificationProperties properties;

    public OutboxSweeper(NotificationOutboxRepository outboxRepository,
                         NotificationDeliveryService deliveryService,
                         NotificationProperties properties) {
        this.outboxRepository = outboxRepository;
        this.deliveryService = deliveryService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.notification.sweep-interval-ms:2000}")
    public void sweep() {
        List<UUID> claimed = claim();
        if (claimed.isEmpty()) {
            return;
        }
        log.debug("Sweeping {} pending notification(s)", claimed.size());
        deliveryService.deliver(claimed);
    }

    /**
     * Reserves work in its own short transaction, so the row locks are released before the
     * slow network calls begin. Holding a database lock for the duration of an FCM request
     * would let one sluggish provider call stall the whole table.
     */
    @Transactional
    public List<UUID> claim() {
        return outboxRepository.claimDueBatch(properties.batchSize());
    }

    /** Returns abandoned in-flight rows to the queue after a crash or restart. */
    @Scheduled(fixedDelay = 120_000L)
    @Transactional
    public void requeueStuck() {
        int requeued = outboxRepository.requeueStuck(Instant.now().minus(STUCK_AFTER));
        if (requeued > 0) {
            log.info("Requeued {} notification(s) stuck in PROCESSING", requeued);
        }
    }

    /** Keeps the table from growing without bound; the feed only needs recent history. */
    @Scheduled(cron = "0 45 3 * * *")
    @Transactional
    public void purgeOld() {
        outboxRepository.deleteOlderThan(Instant.now().minus(Duration.ofDays(90)));
    }
}
