package com.school.canteen.exception;

import org.springframework.http.HttpStatus;

/** An order operation isn't valid for the order's current status. Maps to HTTP 409. */
public class InvalidOrderStateException extends ApiException {

    public InvalidOrderStateException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
