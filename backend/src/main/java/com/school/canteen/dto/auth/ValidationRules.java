package com.school.canteen.dto.auth;

/**
 * Shared validation constants so every endpoint enforces the same rules.
 *
 * Keeping them in one place prevents the class of bug where the client accepts a value the
 * server later rejects — for example a password the sign-up form allowed but the API
 * refused with a confusing 400.
 */
public final class ValidationRules {

    /** Indian mobile numbers: exactly ten digits, first digit 6-9. */
    public static final String MOBILE_PATTERN = "^[6-9]\\d{9}$";
    public static final String MOBILE_MESSAGE = "must be a valid 10-digit mobile number";

    public static final int PASSWORD_MIN = 8;
    /** BCrypt silently truncates beyond 72 bytes, so reject longer input outright. */
    public static final int PASSWORD_MAX = 72;
    public static final String PASSWORD_MESSAGE =
            "must be between 8 and 72 characters";

    private ValidationRules() {
    }
}
