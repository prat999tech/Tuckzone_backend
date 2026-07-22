package com.school.canteen.exception;

import org.springframework.http.HttpStatus;

/**
 * Base type for all expected, client-facing errors. Carries the HTTP status the API
 * should return, so the global handler can translate any subclass uniformly.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
