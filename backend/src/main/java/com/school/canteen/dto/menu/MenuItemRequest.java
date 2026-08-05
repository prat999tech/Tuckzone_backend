package com.school.canteen.dto.menu;

import com.school.canteen.enums.MenuType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record MenuItemRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @NotNull @DecimalMin(value = "0.0") @Digits(integer = 6, fraction = 2) BigDecimal price,
        /** Cost to make one unit; drives profit reporting. Optional. */
        @DecimalMin(value = "0.0") @Digits(integer = 6, fraction = 2) BigDecimal costPrice,
        @NotNull MenuType menuType,
        /** Out-of-stock toggle. Meaningful mainly for FIXED items — DAILY items are
         *  additionally gated per-date via their DailyMenuItem row regardless of this flag. */
        @NotNull Boolean available,
        @Size(max = 500) String imageUrl,
        @Size(max = 255) String allergens) {
}
