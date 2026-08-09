package com.school.canteen.exception;

import org.springframework.http.HttpStatus;

/**
 * Raised when an order (or anything else scoped to a menu date) is attempted for a date
 * whose ordering window is closed — its cutoff already passed, or the canteen closed it
 * manually. Distinguished from an ordinary error by {@link #CODE} so the client knows to
 * re-fetch the current default ordering date rather than just showing the message and
 * stopping — matching on the human-readable message would break the moment the wording
 * changes. Maps to 409: the request was well-formed, but the moment for it has passed.
 */
public class OrderingClosedException extends ApiException {

    public static final String CODE = "ORDERING_CLOSED";

    public OrderingClosedException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
