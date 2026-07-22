package com.school.canteen.enums;

/** Lifecycle of a wallet top-up. Only a CREATED top-up can transition to PAID (exactly
 *  once) or FAILED — this is what makes crediting idempotent. */
public enum TopupStatus {
    CREATED,
    PAID,
    FAILED
}
