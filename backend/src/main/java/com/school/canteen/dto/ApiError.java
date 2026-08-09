package com.school.canteen.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Uniform error body returned for every failed request. A record because it is an
 * immutable data carrier — no behaviour, just fields.
 *
 * @param details field-level messages (e.g. validation errors); empty for simple errors.
 *                Clients display these, so never put machine-readable markers here.
 * @param code    optional stable identifier for errors a client must branch on rather than
 *                merely show (e.g. EMAIL_NOT_VERIFIED). Null for ordinary errors. Kept
 *                separate from {@code details} precisely because that field is user-facing.
 * @param fieldErrors field name -> message, for validation failures. Lets a form put each
 *                message on the input that caused it instead of dumping one combined
 *                string into a toast. Null when the error is not field-specific.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details,
        String code,
        Map<String, String> fieldErrors) {
}
