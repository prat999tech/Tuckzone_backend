package com.school.canteen.enums;

/** How a notification reaches the user. */
public enum NotificationChannel {
    /** Firebase Cloud Messaging to the user's registered devices. */
    PUSH,
    /** In-app feed, readable inside the application itself. */
    IN_APP,
    /** Email — reserved for receipts and security-sensitive events, not every status ping. */
    EMAIL
}
