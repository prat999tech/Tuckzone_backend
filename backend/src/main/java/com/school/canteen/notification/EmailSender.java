package com.school.canteen.notification;

/**
 * Delivers a single email (Strategy pattern, mirroring {@link PushSender}).
 *
 * Unlike push, email is not batched here: only a handful of event types are email-worthy
 * (see {@code NotificationServiceImpl}), so volume never approaches the point where
 * batching would matter, and providers like Resend charge and rate-limit per call anyway.
 */
public interface EmailSender {

    /**
     * Sends the message. Must never throw — a provider outage is reported as
     * {@code false} so the outbox can retry it, not as an exception that could escape into
     * unrelated request handling.
     */
    boolean send(EmailMessage message);
}
