package com.school.canteen.dto.notification;

import com.school.canteen.enums.NotificationEvent;
import java.time.Instant;
import java.util.UUID;

/** An entry in the user's in-app notification feed. */
public record NotificationResponse(
        UUID id,
        NotificationEvent event,
        String title,
        String body,
        String payload,
        Instant createdAt) {
}
