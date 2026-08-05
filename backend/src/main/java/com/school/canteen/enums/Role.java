package com.school.canteen.enums;

/**
 * A user has exactly one role. Stored as a string in the DB (EnumType.STRING) so the
 * column stays human-readable and reordering this enum can never corrupt existing rows.
 *
 * There is deliberately no school-administrator role: the canteen is vendor-operated and
 * the school does not own or moderate any part of it.
 *
 * SUB_ADMIN is a restricted canteen-staff role: incoming orders, menu management and order
 * export only. It can never manage accounts (including other SUB_ADMIN accounts) or touch
 * business/payment settings — see the per-endpoint @PreAuthorize checks throughout the admin
 * controllers.
 */
public enum Role {
    STUDENT,
    PARENT,
    TEACHER,
    CANTEEN_ADMIN,
    SUB_ADMIN
}
