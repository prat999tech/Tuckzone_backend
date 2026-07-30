package com.school.canteen.exception;

import org.springframework.http.HttpStatus;

/**
 * Raised when someone signs in with a password before confirming the address they
 * registered with.
 *
 * Carries the email so the client can jump straight to the verification screen with the
 * field already filled in, and is distinguished from every other 403 by the
 * {@link #CODE} the handler puts in the error body's {@code details} — matching on the
 * human-readable message would break the moment the wording changes.
 */
public class EmailNotVerifiedException extends ApiException {

    /** Machine-readable marker the mobile app keys off; see GlobalExceptionHandler. */
    public static final String CODE = "EMAIL_NOT_VERIFIED";

    private final String email;

    public EmailNotVerifiedException(String email) {
        super(HttpStatus.FORBIDDEN,
                "Please verify your email address to finish setting up your account.");
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
