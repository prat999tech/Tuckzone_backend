package com.school.canteen.notification;

import java.util.List;
import java.util.UUID;

/**
 * Signals that outbox rows were written and are ready to send.
 *
 * Carries only ids, never entities: the listener runs on another thread after the
 * transaction commits, where a detached entity would be unusable.
 */
public record NotificationsQueuedEvent(List<UUID> outboxIds) {
}
