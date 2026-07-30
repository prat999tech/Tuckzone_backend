package com.school.canteen.dto.menu;

import com.school.canteen.enums.FoodType;
import com.school.canteen.enums.MenuCategory;
import java.math.BigDecimal;
import java.util.UUID;

public record MenuItemResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        BigDecimal costPrice,
        FoodType foodType,
        MenuCategory category,
        String imageUrl,
        String allergens,
        boolean active) {
}
