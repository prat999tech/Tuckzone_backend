package com.school.canteen.service.impl;

import tools.jackson.databind.ObjectMapper;
import com.school.canteen.entity.NotificationOutbox;
import com.school.canteen.entity.User;
import com.school.canteen.enums.NotificationChannel;
import com.school.canteen.enums.NotificationEvent;
import com.school.canteen.enums.OutboxStatus;
import com.school.canteen.enums.Role;
import com.school.canteen.enums.UserStatus;
import com.school.canteen.notification.NotificationsQueuedEvent;
import com.school.canteen.repository.NotificationOutboxRepository;
import com.school.canteen.repository.UserRepository;
import com.school.canteen.service.NotificationService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    /** Customers who should hear canteen-wide announcements. Admins are excluded. */
    private static final List<Role> CUSTOMER_ROLES =
            List.of(Role.STUDENT, Role.TEACHER, Role.PARENT);

    /** Users pulled per page when broadcasting, so a 5k-user announcement never loads
     *  every account into memory at once. */
    private static final int BROADCAST_PAGE_SIZE = 500;

    private final NotificationOutboxRepository outboxRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public NotificationServiceImpl(NotificationOutboxRepository outboxRepository,
                                   UserRepository userRepository,
                                   ApplicationEventPublisher eventPublisher,
                                   ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public int notifyAllCustomers(NotificationEvent event, String title, String body,
                                  Map<String, String> data) {
        String payload = serialise(data);
        Instant now = Instant.now();
        List<UUID> pushIds = new ArrayList<>();
        int notified = 0;

        for (Role role : CUSTOMER_ROLES) {
            int page = 0;
            while (true) {
                List<User> batch = userRepository.findByRoleAndStatus(role, UserStatus.ACTIVE,
                        PageRequest.of(page, BROADCAST_PAGE_SIZE));
                if (batch.isEmpty()) {
                    break;
                }
                for (User user : batch) {
                    // Broadcasts stay push + in-app: emailing every customer on every
                    // open/close announcement would look like spam to mail providers and
                    // risks the sending domain's reputation, which would then break the
                    // per-user emails that actually matter (codes, receipts).
                    pushIds.add(queueFor(user, event, title, body, payload, now));
                    notified++;
                }
                if (batch.size() < BROADCAST_PAGE_SIZE) {
                    break;
                }
                page++;
            }
        }

        if (!pushIds.isEmpty()) {
            // One event for the whole broadcast: the delivery service then batches these
            // into FCM calls of up to 500 rather than one call per recipient.
            eventPublisher.publishEvent(new NotificationsQueuedEvent(pushIds));
        }
        return notified;
    }

    /** Writes the in-app + push pair for one user and returns the push row's id. */
    private UUID queueFor(User user, NotificationEvent event, String title, String body,
                          String payload, Instant now) {
        NotificationOutbox inApp = row(user, event, NotificationChannel.IN_APP, title, body, payload, now);
        inApp.setStatus(OutboxStatus.SENT);
        inApp.setSentAt(now);
        outboxRepository.save(inApp);

        NotificationOutbox push = row(user, event, NotificationChannel.PUSH, title, body, payload, now);
        push.setStatus(OutboxStatus.PENDING);
        outboxRepository.save(push);
        return push.getId();
    }

    @Override
    @Transactional
    public void notifyUser(User user, NotificationEvent event, String title, String body,
                           Map<String, String> data) {
        String payload = serialise(data);
        Instant now = Instant.now();

        // The in-app record IS its own delivery — it exists to be read from the database,
        // so there is nothing to send and it starts life as SENT.
        NotificationOutbox inApp = row(user, event, NotificationChannel.IN_APP, title, body, payload, now);
        inApp.setStatus(OutboxStatus.SENT);
        inApp.setSentAt(now);

        // The push copy is what the pipeline actually delivers.
        NotificationOutbox push = row(user, event, NotificationChannel.PUSH, title, body, payload, now);
        push.setStatus(OutboxStatus.PENDING);

        outboxRepository.save(inApp);
        outboxRepository.save(push);

        List<UUID> pending = new ArrayList<>();
        pending.add(push.getId());

        // Every per-user event also goes by email: it is the channel the user is
        // guaranteed to have (push needs the app installed and permission granted).
        NotificationOutbox email = row(user, event, NotificationChannel.EMAIL, title, body, payload, now);
        email.setStatus(OutboxStatus.PENDING);
        outboxRepository.save(email);
        pending.add(email.getId());

        // Published, not sent, here. The listener fires only after this transaction
        // commits, so we never announce something that later rolled back.
        eventPublisher.publishEvent(new NotificationsQueuedEvent(pending));
    }

    private NotificationOutbox row(User user, NotificationEvent event, NotificationChannel channel,
                                   String title, String body, String payload, Instant now) {
        NotificationOutbox outbox = new NotificationOutbox();
        outbox.setUser(user);
        outbox.setEventType(event);
        outbox.setChannel(channel);
        outbox.setTitle(title);
        outbox.setBody(body);
        outbox.setPayload(payload);
        outbox.setAttempts(0);
        outbox.setNextAttemptAt(now);
        return outbox;
    }

    private String serialise(Map<String, String> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (RuntimeException ex) {
            // Jackson 3 (shipped with Spring Boot 4) throws unchecked exceptions.
            // A malformed payload must never abort the business transaction that triggered
            // it — losing the deep-link data is far better than losing the order.
            log.warn("Could not serialise notification payload; sending without it", ex);
            return null;
        }
    }
}
