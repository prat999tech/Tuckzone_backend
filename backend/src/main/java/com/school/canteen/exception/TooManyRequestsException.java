package com.school.canteen.exception;

import org.springframework.http.HttpStatus;

/** Rate limit exceeded. Maps to HTTP 429. */
public class TooManyRequestsException extends ApiException {

    public TooManyRequestsException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, message);
    }
}
