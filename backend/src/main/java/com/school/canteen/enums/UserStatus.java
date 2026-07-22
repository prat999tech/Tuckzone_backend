package com.school.canteen.enums;

/**
 * Registration lifecycle. A user cannot place orders until APPROVED — this is the trust
 * gate that compensates for having no official school data to verify against.
 */
public enum UserStatus {
    PENDING,
    APPROVED,
    REJECTED,
    DISABLED
}
