package com.school.canteen.notification;

import java.util.Map;

/**
 * A single push addressed to one device.
 *
 * @param data key/value pairs the mobile app reads to deep-link (order id, status, ...)
 */
public record PushMessage(
        String token,
        String title,
        String body,
        Map<String, String> data) {
}
