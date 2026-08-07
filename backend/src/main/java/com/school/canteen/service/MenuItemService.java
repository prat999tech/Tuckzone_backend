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

    /** Soft delete (Retire): marks the item inactive so history stays intact. Hidden from
     *  students, still visible to admin under the Retired filter. */
    void deactivate(UUID id);

    /** Reverses {@link #deactivate}: brings a retired item back to Active. */
    MenuItemResponse activate(UUID id);

    /**
     * Hard delete: permanently removes the row. Rejected if any order has ever contained
     * this item — that history must never be destroyed — so this only succeeds for an item
     * nobody has ordered yet. Any daily-menu scheduling entries for it are cleaned up first
     * since those are disposable, unlike order history.
     */
    void permanentlyDelete(UUID id);

    MenuItemResponse get(UUID id);

    /** @param menuType optional filter so the Daily/Fixed admin sections can each fetch
     *  just their own catalog items. */
    List<MenuItemResponse> list(boolean includeInactive, MenuType menuType);

    /** The Daily Delights as customers see it: active, in-stock FIXED items, optionally
     *  name-filtered. No date dimension — unlike Meal of the Day, these need no scheduling. */
    List<MenuItemResponse> listFixedMenu(String query);
}
