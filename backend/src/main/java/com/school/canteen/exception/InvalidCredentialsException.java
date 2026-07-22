package com.school.canteen.exception;

import org.springframework.http.HttpStatus;

/**
 * Wrong email or password. Deliberately does NOT distinguish between "no such email" and
 * "bad password" — a single generic 401 prevents attackers enumerating valid accounts.
 */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }
}
