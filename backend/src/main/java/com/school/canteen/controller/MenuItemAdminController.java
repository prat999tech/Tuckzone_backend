package com.school.canteen.controller;

import com.school.canteen.dto.menu.MenuItemRequest;
import com.school.canteen.dto.menu.MenuItemResponse;
import com.school.canteen.service.MenuItemService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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

/** Canteen-admin CRUD over the permanent catalog. */
@RestController
@RequestMapping("/api/admin/menu-items")
@PreAuthorize("hasRole('CANTEEN_ADMIN')")
public class MenuItemAdminController {

    private final MenuItemService menuItemService;

    public MenuItemAdminController(MenuItemService menuItemService) {
        this.menuItemService = menuItemService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItemResponse create(@Valid @RequestBody MenuItemRequest request) {
        return menuItemService.create(request);
    }

    @GetMapping
    public List<MenuItemResponse> list(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return menuItemService.list(includeInactive);
    }

    @GetMapping("/{id}")
    public MenuItemResponse get(@PathVariable UUID id) {
        return menuItemService.get(id);
    }

    @PutMapping("/{id}")
    public MenuItemResponse update(@PathVariable UUID id,
                                   @Valid @RequestBody MenuItemRequest request) {
        return menuItemService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID id) {
        menuItemService.deactivate(id);
    }
}
