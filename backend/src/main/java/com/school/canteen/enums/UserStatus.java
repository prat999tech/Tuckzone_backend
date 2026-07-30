package com.school.canteen.enums;

/**
 * Whether an account may be used.
 *
 * Registration no longer waits for approval — a parent proves a child relationship with
 * the admission number plus the recorded parent mobile, which is the actual trust anchor.
 * DISABLED remains so the canteen can block a fraudulent or abusive account, and it takes
 * effect immediately: the JWT filter re-checks it on every request.
 */
public enum UserStatus {
    ACTIVE,
    DISABLED
}
