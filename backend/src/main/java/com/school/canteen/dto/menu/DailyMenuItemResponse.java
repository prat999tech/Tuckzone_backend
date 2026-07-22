package com.school.canteen.dto.menu;

import java.time.LocalDate;
import java.util.UUID;

public record DailyMenuItemResponse(
        UUID id,
        LocalDate menuDate,
        MenuItemResponse menuItem,
        int totalQuantity,
        int remainingQuantity,
        boolean available) {
}
