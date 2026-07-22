package com.school.canteen.dto.menu;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

/** Add a catalog item to a date's menu with a starting quantity. */
public record DailyMenuItemRequest(
        @NotNull LocalDate menuDate,
        @NotNull UUID menuItemId,
        @NotNull @Min(0) Integer totalQuantity) {
}
