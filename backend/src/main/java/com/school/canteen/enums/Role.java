package com.school.canteen.enums;

/**
 * A user has exactly one role. Stored as a string in the DB (EnumType.STRING) so the
 * column stays human-readable and reordering this enum can never corrupt existing rows.
 *
 * There is deliberately no school-administrator role: the canteen is vendor-operated and
 * the school does not own or moderate any part of it.
 */
public enum Role {
    STUDENT,
    PARENT,
    TEACHER,
    CANTEEN_ADMIN
}
