package com.school.canteen.exception;

import org.springframework.http.HttpStatus;

/**
 * The presented Firebase ID token failed Admin SDK verification — wrong project, expired,
 * revoked, malformed, or simply not a real Firebase token. Deliberately generic: the exact
 * SDK-level reason (see the wrapped cause, logged but never returned) is not the caller's
 * business, same rationale as {@link InvalidCredentialsException}.
 */
public class InvalidFirebaseTokenException extends ApiException {

    public InvalidFirebaseTokenException() {
        super(HttpStatus.UNAUTHORIZED, "Invalid or expired Firebase session. Please sign in again.");
    }
}
