package com.school.canteen.enums;

/**
 * Which of the two menu sections an item belongs to. DAILY items rotate day to day and are
 * only orderable once scheduled onto a specific date via {@link com.school.canteen.entity.DailyMenuItem}.
 * FIXED items (drinks, snacks, etc.) are orderable every day once active and available,
 * with no per-date scheduling required.
 */
public enum MenuType {
    DAILY,
    FIXED
}
