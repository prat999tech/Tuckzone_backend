package com.school.canteen.controller;

import com.school.canteen.dto.menu.DailyMenuItemResponse;
import com.school.canteen.enums.FoodType;
import com.school.canteen.enums.MenuCategory;
import com.school.canteen.service.DailyMenuService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The menu as customers see it: only available items whose catalog entry is active, with
 * optional veg/category/search filters. Any authenticated user may read it. Defaults to
 * today when no date is supplied.
 */
@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final DailyMenuService dailyMenuService;

    public MenuController(DailyMenuService dailyMenuService) {
        this.dailyMenuService = dailyMenuService;
    }

    @GetMapping
    public List<DailyMenuItemResponse> menu(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) FoodType foodType,
            @RequestParam(required = false) MenuCategory category,
            @RequestParam(required = false) String q) {
        LocalDate menuDate = (date != null) ? date : LocalDate.now();
        return dailyMenuService.getMenu(menuDate, foodType, category, q);
    }
}
