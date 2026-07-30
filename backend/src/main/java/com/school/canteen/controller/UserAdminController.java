package com.school.canteen.controller;

import com.school.canteen.dto.UserSummary;
import com.school.canteen.enums.Role;
import com.school.canteen.service.UserAdminService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Account administration for the canteen operator. There is no approval queue any more —
 * only the ability to block or restore an account.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('CANTEEN_ADMIN')")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public List<UserSummary> list(@RequestParam(required = false) Role role,
                                  @RequestParam(required = false) Integer page,
                                  @RequestParam(required = false) Integer size) {
        return userAdminService.listUsers(role, page, size);
    }

    @PostMapping("/{id}/disable")
    public UserSummary disable(@PathVariable UUID id) {
        return userAdminService.disable(id);
    }

    @PostMapping("/{id}/enable")
    public UserSummary enable(@PathVariable UUID id) {
        return userAdminService.enable(id);
    }
}
