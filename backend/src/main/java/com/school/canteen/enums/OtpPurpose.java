package com.school.canteen.enums;

/**
 * What an OTP authorises. A code is bound to one purpose so a passcode obtained for
 * signing in can never be replayed to change someone's password.
 */
public enum OtpPurpose {
    LOGIN,
    PASSWORD_RESET,
    /** Confirms the address supplied at registration actually belongs to the user. */
    EMAIL_VERIFICATION
}
