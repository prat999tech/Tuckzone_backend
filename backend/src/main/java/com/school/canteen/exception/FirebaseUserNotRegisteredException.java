package com.school.canteen.exception;

import org.springframework.http.HttpStatus;

/**
 * The Firebase identity is genuinely verified but matches no existing application account
 * (no {@code firebase_uid} link, and no existing user with the same email/mobile to
 * auto-link to). Distinguished by {@link #CODE} so the client can route straight to the
 * "complete your registration" screen instead of showing a generic error — mirrors how
 * {@link EmailNotVerifiedException} lets the client react to a specific auth outcome.
 */
public class FirebaseUserNotRegisteredException extends ApiException {

    public static final String CODE = "FIREBASE_USER_NOT_REGISTERED";

    public FirebaseUserNotRegisteredException() {
        super(HttpStatus.NOT_FOUND, "No account found for this Firebase identity. Please complete registration.");
    }
}
