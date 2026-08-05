package com.school.canteen.service;

import com.school.canteen.dto.menu.MenuItemRequest;
import com.school.canteen.dto.menu.MenuItemResponse;
import com.school.canteen.enums.MenuType;
import java.util.List;
import java.util.UUID;

/** Catalog management for the canteen admin. */
public interface MenuItemService {

    MenuItemResponse create(MenuItemRequest request);

    MenuItemResponse update(UUID id, MenuItemRequest request);

    /** Soft delete: marks the item inactive so history stays intact. */
    void deactivate(UUID id);

    MenuItemResponse get(UUID id);

    /** @param menuType optional filter so the Daily/Fixed admin sections can each fetch
     *  just their own catalog items. */
    List<MenuItemResponse> list(boolean includeInactive, MenuType menuType);

    /** The Daily Delights as customers see it: active, in-stock FIXED items, optionally
     *  name-filtered. No date dimension — unlike Meal of the Day, these need no scheduling. */
    List<MenuItemResponse> listFixedMenu(String query);
}
