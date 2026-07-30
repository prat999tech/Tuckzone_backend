package com.school.canteen.notification;

import java.util.List;

/**
 * Delivers push notifications (Strategy pattern).
 *
 * The API is deliberately batch-shaped: FCM accepts up to 500 distinct messages in a
 * single call, so a rush of orders costs one or two network round trips instead of
 * hundreds. Implementations must return one outcome per input message, in the same order.
 */
public interface PushSender {

    List<PushOutcome> sendAll(List<PushMessage> messages);

    /** Largest batch this provider accepts in one call. */
    int maxBatchSize();
}
