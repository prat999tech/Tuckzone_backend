package com.school.canteen.exception;

import org.springframework.http.HttpStatus;

/** A semantically invalid request that validation annotations can't express. Maps to 400. */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
