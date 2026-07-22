package com.school.canteen.dto.menu;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Adjust a day's stock or toggle availability. */
public record DailyMenuUpdateRequest(
        @NotNull @Min(0) Integer totalQuantity,
        @NotNull Boolean available) {
}
