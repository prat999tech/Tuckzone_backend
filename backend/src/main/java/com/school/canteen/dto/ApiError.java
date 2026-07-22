package com.school.canteen.dto;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error body returned for every failed request. A record because it is an
 * immutable data carrier — no behaviour, just fields.
 *
 * @param details field-level messages (e.g. validation errors); empty for simple errors.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details) {
}
