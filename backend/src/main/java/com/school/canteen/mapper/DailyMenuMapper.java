package com.school.canteen.mapper;

import com.school.canteen.dto.menu.DailyMenuItemResponse;
import com.school.canteen.entity.DailyMenuItem;
import org.springframework.stereotype.Component;

@Component
public class DailyMenuMapper {

    private final MenuItemMapper menuItemMapper;

    public DailyMenuMapper(MenuItemMapper menuItemMapper) {
        this.menuItemMapper = menuItemMapper;
    }

    public DailyMenuItemResponse toResponse(DailyMenuItem entry) {
        return new DailyMenuItemResponse(
                entry.getId(),
                entry.getMenuDate(),
                menuItemMapper.toResponse(entry.getMenuItem()),
                entry.getTotalQuantity(),
                entry.getRemainingQuantity(),
                entry.isAvailable());
    }
}
