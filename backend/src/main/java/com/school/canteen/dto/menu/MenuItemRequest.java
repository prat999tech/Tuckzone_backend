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
        /**
         * Deliberately absent: an image is set only via {@code POST .../{id}/image}
         * (multipart upload), never by an admin pasting a URL — see MenuItemService.
         * uploadImage. A legacy item's existing {@code image_url} is preserved automatically
         * (this request never touches it) until that item gets a real upload.
         */
        @Size(max = 255) String allergens) {
}
