package com.school.canteen.controller;

import com.school.canteen.dto.menu.DailyMenuItemResponse;
import com.school.canteen.dto.menu.MenuItemResponse;
import com.school.canteen.service.DailyMenuService;
import com.school.canteen.service.MenuItemService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The menu as customers see it, split into the two sections the app now has: Meal of the Day
 * (date-scheduled, rotates day to day) and Daily Delights (always orderable once active and in
 * stock, no date dimension). Any authenticated user may read either. Daily defaults to
 * today when no date is supplied.
 */
@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final DailyMenuService dailyMenuService;
    private final MenuItemService menuItemService;
    private final Clock clock;

    public MenuController(DailyMenuService dailyMenuService, MenuItemService menuItemService, Clock clock) {
        this.dailyMenuService = dailyMenuService;
        this.menuItemService = menuItemService;
        this.clock = clock;
    }

    @GetMapping("/daily")
    public List<DailyMenuItemResponse> daily(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String q) {
        // "Today" in the school's timezone. With the JVM default (UTC on the deploy host)
        // the menu would roll over to the next day at 5:30 AM local time.
        LocalDate menuDate = (date != null) ? date : LocalDate.now(clock);
        return dailyMenuService.getMenu(menuDate, q);
    }

    @GetMapping("/fixed")
    public List<MenuItemResponse> fixed(@RequestParam(required = false) String q) {
        return menuItemService.listFixedMenu(q);
    }
}
