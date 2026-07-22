package com.school.canteen.exception;

import org.springframework.http.HttpStatus;

/** Credentials were correct, but the account is not APPROVED yet. Maps to HTTP 403. */
public class AccountNotActiveException extends ApiException {

    public AccountNotActiveException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
