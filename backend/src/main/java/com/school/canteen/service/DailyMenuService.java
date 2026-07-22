package com.school.canteen.service;

import com.school.canteen.dto.menu.DailyMenuItemRequest;
import com.school.canteen.dto.menu.DailyMenuItemResponse;
import com.school.canteen.dto.menu.DailyMenuUpdateRequest;
import com.school.canteen.enums.FoodType;
import com.school.canteen.enums.MenuCategory;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DailyMenuService {

    // --- canteen admin ---
    DailyMenuItemResponse addItem(DailyMenuItemRequest request);

    DailyMenuItemResponse update(UUID id, DailyMenuUpdateRequest request);

    void remove(UUID id);

    List<DailyMenuItemResponse> listForDate(LocalDate date);

    // --- public (any authenticated customer) ---
    List<DailyMenuItemResponse> getMenu(LocalDate date, FoodType foodType,
                                        MenuCategory category, String query);
}
