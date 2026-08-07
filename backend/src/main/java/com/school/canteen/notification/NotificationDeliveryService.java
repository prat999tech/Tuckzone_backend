package com.school.canteen.notification;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.school.canteen.config.NotificationProperties;
import com.school.canteen.entity.DeviceToken;
import com.school.canteen.entity.NotificationOutbox;
import com.school.canteen.enums.NotificationChannel;
import com.school.canteen.enums.OutboxStatus;
import com.school.canteen.repository.DeviceTokenRepository;
import com.school.canteen.repository.NotificationOutboxRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns claimed outbox rows into actual deliveries — push or email.
 *
 * Shared by both delivery paths — the immediate after-commit attempt and the scheduled
 * sweeper — so retry behaviour and bookkeeping cannot drift apart between them.
 */
@Service
public class NotificationDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryService.class);

    private final NotificationOutboxRepository outboxRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final PushSender pushSender;
    private final EmailSender emailSender;
    private final NotificationProperties properties;
    private final ObjectMapper objectMapper;

    public NotificationDeliveryService(NotificationOutboxRepository outboxRepository,
                                       DeviceTokenRepository deviceTokenRepository,
                                       PushSender pushSender,
                                       EmailSender emailSender,
                                       NotificationProperties properties,
                                       ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.deviceTokenRepository = deviceTokenRepository;
        this.pushSender = pushSender;
        this.emailSender = emailSender;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Sends the given outbox rows, whatever their channel.
     *
     * REQUIRES_NEW because the immediate path runs after the business transaction has
     * already committed: this delivery bookkeeping needs a transaction of its own, and a
     * failure here must never be able to affect the order it is describing.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deliver(List<UUID> outboxIds) {
        if (outboxIds.isEmpty()) {
            return;
        }
        List<NotificationOutbox> rows = outboxRepository.findAllById(outboxIds).stream()
                .filter(row -> row.getStatus() == OutboxStatus.PENDING
                        || row.getStatus() == OutboxStatus.PROCESSING)
                .toList();
        if (rows.isEmpty()) {
            return; // already handled by the other path
        }

        Instant now = Instant.now();
        List<NotificationOutbox> pushRows = new ArrayList<>();
        List<NotificationOutbox> emailRows = new ArrayList<>();
        for (NotificationOutbox row : rows) {
            if (row.getChannel() == NotificationChannel.PUSH) {
                pushRows.add(row);
            } else if (row.getChannel() == NotificationChannel.EMAIL) {
                emailRows.add(row);
            }
            // IN_APP rows are never queued for delivery — they are written as SENT at
            // creation, since the row itself is the delivery.
        }

        if (!pushRows.isEmpty()) {
            deliverPush(pushRows, now);
        }
        if (!emailRows.isEmpty()) {
            deliverEmail(emailRows, now);
        }
    }

    private void deliverPush(List<NotificationOutbox> rows, Instant now) {
        // Resolve every recipient's devices up front: one query for the whole batch rather
        // than one per notification.
        Map<UUID, List<DeviceToken>> devicesByUser = new HashMap<>();
        for (NotificationOutbox row : rows) {
            devicesByUser.computeIfAbsent(row.getUser().getId(), deviceTokenRepository::findByUser_Id);
        }

        List<PushMessage> messages = new ArrayList<>();
        List<NotificationOutbox> messageOwners = new ArrayList<>();

        for (NotificationOutbox row : rows) {
            List<DeviceToken> devices = devicesByUser.getOrDefault(row.getUser().getId(), List.of());
            if (devices.isEmpty()) {
                // Nothing to push to (user has not installed the app, or logged out of
                // every device). The in-app copy still exists, so treat this as done
                // rather than retrying forever against a user with no devices.
                markSent(row, now);
                continue;
            }
            Map<String, String> data = deserialise(row.getPayload());
            for (DeviceToken device : devices) {
                messages.add(new PushMessage(device.getToken(), row.getTitle(), row.getBody(), data));
                messageOwners.add(row);
            }
        }

        if (messages.isEmpty()) {
            return;
        }

        // One provider call per chunk instead of one per message.
        int chunkSize = Math.max(1, Math.min(properties.batchSize(), pushSender.maxBatchSize()));
        Set<String> deadTokens = new HashSet<>();
        Map<UUID, Boolean> anySuccessByRow = new HashMap<>();
        Map<UUID, String> lastErrorByRow = new HashMap<>();

        for (int start = 0; start < messages.size(); start += chunkSize) {
            int end = Math.min(start + chunkSize, messages.size());
            List<PushMessage> chunk = messages.subList(start, end);
            List<PushOutcome> outcomes = safeSendPush(chunk);

            for (int i = 0; i < chunk.size(); i++) {
                PushOutcome outcome = outcomes.get(i);
                NotificationOutbox owner = messageOwners.get(start + i);
                anySuccessByRow.merge(owner.getId(), outcome.success(), Boolean::logicalOr);
                if (!outcome.success()) {
                    lastErrorByRow.put(owner.getId(), outcome.error());
                    if (outcome.tokenInvalid()) {
                        deadTokens.add(chunk.get(i).token());
                    }
                }
            }
        }

        for (NotificationOutbox row : rows) {
            if (row.getStatus() == OutboxStatus.SENT) {
                continue; // handled above (no devices)
            }
            // Delivered to at least one of the user's devices counts as delivered: the
            // alternative would resend to a working phone just because a second, stale
            // device failed.
            if (Boolean.TRUE.equals(anySuccessByRow.get(row.getId()))) {
                markSent(row, now);
            } else {
                markFailed(row, lastErrorByRow.get(row.getId()), now);
            }
        }

        if (!deadTokens.isEmpty()) {
            // Stop spending batch slots on devices that will never receive anything.
            int removed = deviceTokenRepository.deleteByTokenIn(deadTokens);
            log.info("Removed {} dead device token(s)", removed);
        }
    }

    /**
     * Sends each email individually rather than batching.
     *
     * Only a handful of event types ever reach this method (see the allowlist in
     * NotificationServiceImpl), so volume never approaches the point batching would help,
     * and most email providers bill and rate-limit per call regardless.
     */
    private void deliverEmail(List<NotificationOutbox> rows, Instant now) {
        for (NotificationOutbox row : rows) {
            String to = row.getUser().getEmail();
            EmailMessage message = new EmailMessage(
                    to, row.getTitle(), toHtml(row.getTitle(), row.getBody()), row.getBody());
            boolean delivered = safeSendEmail(message);
            if (delivered) {
                markSent(row, now);
            } else {
                markFailed(row, "email provider rejected or failed the send", now);
            }
        }
    }

    private List<PushOutcome> safeSendPush(List<PushMessage> chunk) {
        try {
            List<PushOutcome> outcomes = pushSender.sendAll(chunk);
            if (outcomes.size() == chunk.size()) {
                return outcomes;
            }
            log.warn("Push provider returned {} outcomes for {} messages", outcomes.size(), chunk.size());
        } catch (RuntimeException ex) {
            // A provider outage must leave the rows retryable, not blow up the caller.
            log.warn("Push delivery failed for a batch of {}", chunk.size(), ex);
            return chunk.stream().map(m -> PushOutcome.failed(ex.getMessage())).toList();
        }
        return chunk.stream().map(m -> PushOutcome.failed("provider returned no result")).toList();
    }

    private boolean safeSendEmail(EmailMessage message) {
        try {
            return emailSender.send(message);
        } catch (RuntimeException ex) {
            // Defensive: EmailSender implementations are documented not to throw, but a
            // provider outage here must still leave the row retryable, not blow up the
            // sweeper or the request thread that triggered the immediate send.
            log.warn("Email delivery failed for {}", message.to(), ex);
            return false;
        }
    }

    private void markSent(NotificationOutbox row, Instant now) {
        row.setStatus(OutboxStatus.SENT);
        row.setSentAt(now);
        row.setLastError(null);
    }

    private void markFailed(NotificationOutbox row, String error, Instant now) {
        row.setAttempts(row.getAttempts() + 1);
        row.setLastError(truncate(error));
        if (row.getAttempts() >= properties.maxAttempts()) {
            // Give up rather than retrying forever; the in-app copy is still there.
            row.setStatus(OutboxStatus.SKIPPED);
            log.warn("Giving up on notification {} after {} attempts", row.getId(), row.getAttempts());
            return;
        }
        row.setStatus(OutboxStatus.FAILED);
        // Linear backoff: a provider blip should not be hammered every second.
        row.setNextAttemptAt(now.plus(
                Duration.ofSeconds((long) properties.retryBackoffSeconds() * row.getAttempts())));
    }

    private Map<String, String> deserialise(String payload) {
        if (payload == null || payload.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(payload, new TypeReference<Map<String, String>>() {
            });
        } catch (Exception ex) {
            log.warn("Could not read notification payload; sending without data", ex);
            return Map.of();
        }
    }

    /**
     * Minimal HTML wrapper for an email body. Title and body come from server-generated
     * strings (order numbers, delivery-person names an admin typed, item names) — escaping
     * them keeps admin-entered free text from being interpreted as markup.
     */
    private static String toHtml(String title, String body) {
        return "<div style=\"font-family:sans-serif;font-size:15px;color:#1f2937\">"
                + "<h2 style=\"margin:0 0 12px\">" + escapeHtml(title) + "</h2>"
                + "<p style=\"margin:0\">" + escapeHtml(body) + "</p>"
                + "</div>";
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 480 ? value.substring(0, 480) : value;
    }
}
