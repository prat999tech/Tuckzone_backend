package com.school.canteen.controller;

import com.school.canteen.dto.menu.DailyMenuItemRequest;
import com.school.canteen.dto.menu.DailyMenuItemResponse;
import com.school.canteen.dto.menu.DailyMenuUpdateRequest;
import com.school.canteen.service.DailyMenuService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Canteen-admin management of a given day's offerings and stock. */
@RestController
@RequestMapping("/api/admin/daily-menu")
@PreAuthorize("hasRole('CANTEEN_ADMIN')")
public class DailyMenuAdminController {

    private final DailyMenuService dailyMenuService;

    public DailyMenuAdminController(DailyMenuService dailyMenuService) {
        this.dailyMenuService = dailyMenuService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DailyMenuItemResponse addItem(@Valid @RequestBody DailyMenuItemRequest request) {
        return dailyMenuService.addItem(request);
    }

    @GetMapping
    public List<DailyMenuItemResponse> listForDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dailyMenuService.listForDate(date);
    }

    @PutMapping("/{id}")
    public DailyMenuItemResponse update(@PathVariable UUID id,
                                        @Valid @RequestBody DailyMenuUpdateRequest request) {
        return dailyMenuService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable UUID id) {
        dailyMenuService.remove(id);
    }
}
