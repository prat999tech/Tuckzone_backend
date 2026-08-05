package com.school.canteen.dto.menu;

import com.school.canteen.enums.MenuType;
import java.math.BigDecimal;
import java.util.UUID;

public record MenuItemResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        BigDecimal costPrice,
        MenuType menuType,
        boolean available,
        String imageUrl,
        String allergens,
        boolean active) {
}
