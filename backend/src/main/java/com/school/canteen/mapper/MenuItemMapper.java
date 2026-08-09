package com.school.canteen.mapper;

import com.school.canteen.dto.menu.MenuItemRequest;
import com.school.canteen.dto.menu.MenuItemResponse;
import com.school.canteen.entity.MenuItem;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Component
public class MenuItemMapper {

    public MenuItem toEntity(MenuItemRequest request) {
        MenuItem item = new MenuItem();
        applyEditableFields(item, request);
        return item;
    }

    /** Copies the client-editable fields onto an entity (used for both create and update).
     *  Note it never touches id/active/timestamps/image — the image is set only through
     *  MenuItemService#uploadImage, and this never touches the legacy imageUrl column either. */
    public void applyEditableFields(MenuItem item, MenuItemRequest request) {
        item.setName(request.name());
        item.setDescription(request.description());
        item.setPrice(request.price());
        item.setCostPrice(request.costPrice());
        item.setMenuType(request.menuType());
        item.setAvailable(request.available());
        item.setAllergens(request.allergens());
    }

    public MenuItemResponse toResponse(MenuItem item) {
        return new MenuItemResponse(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.getCostPrice(),
                item.getMenuType(),
                item.isAvailable(),
                resolveImageUrl(item),
                item.getAllergens(),
                item.isActive());
    }

    /**
     * An uploaded photo wins over the legacy external URL when both somehow exist (they
     * never should in practice, but "uploaded" is the more recent/authoritative one if so).
     * Built from the current request's own host/scheme rather than a hardcoded base URL, so
     * this is correct in dev, in prod behind Render's domain, and behind any future one —
     * nothing here needs a configured "public base URL" property to stay right.
     */
    private String resolveImageUrl(MenuItem item) {
        if (item.hasUploadedImage()) {
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/menu-items/{id}/image")
                    .buildAndExpand(item.getId())
                    .toUriString();
        }
        return item.getImageUrl();
    }
}
