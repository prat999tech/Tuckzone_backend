package com.school.canteen.exception;

import org.springframework.http.HttpStatus;

/** Thrown when creating something that must be unique but already exists. Maps to HTTP 409. */
public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
