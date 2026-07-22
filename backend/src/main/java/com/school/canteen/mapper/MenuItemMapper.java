package com.school.canteen.mapper;

import com.school.canteen.dto.menu.MenuItemRequest;
import com.school.canteen.dto.menu.MenuItemResponse;
import com.school.canteen.entity.MenuItem;
import org.springframework.stereotype.Component;

@Component
public class MenuItemMapper {

    public MenuItem toEntity(MenuItemRequest request) {
        MenuItem item = new MenuItem();
        applyEditableFields(item, request);
        return item;
    }

    /** Copies the client-editable fields onto an entity (used for both create and update).
     *  Note it never touches id/active/timestamps — those are controlled by the system. */
    public void applyEditableFields(MenuItem item, MenuItemRequest request) {
        item.setName(request.name());
        item.setDescription(request.description());
        item.setPrice(request.price());
        item.setFoodType(request.foodType());
        item.setCategory(request.category());
        item.setImageUrl(request.imageUrl());
        item.setAllergens(request.allergens());
    }

    public MenuItemResponse toResponse(MenuItem item) {
        return new MenuItemResponse(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.getFoodType(),
                item.getCategory(),
                item.getImageUrl(),
                item.getAllergens(),
                item.isActive());
    }
}
