package com.school.canteen.service;

import com.school.canteen.dto.menu.MenuItemRequest;
import com.school.canteen.dto.menu.MenuItemResponse;
import java.util.List;
import java.util.UUID;

/** Catalog management for the canteen admin. */
public interface MenuItemService {

    MenuItemResponse create(MenuItemRequest request);

    MenuItemResponse update(UUID id, MenuItemRequest request);

    /** Soft delete: marks the item inactive so history stays intact. */
    void deactivate(UUID id);

    MenuItemResponse get(UUID id);

    List<MenuItemResponse> list(boolean includeInactive);
}
