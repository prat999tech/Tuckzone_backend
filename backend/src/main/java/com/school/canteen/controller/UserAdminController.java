package com.school.canteen.controller;

import com.school.canteen.dto.UserSummary;
import com.school.canteen.enums.UserStatus;
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
 * School-administrator console for the approval gate. The class-level @PreAuthorize means
 * every endpoint here requires the SCHOOL_ADMIN role — enforced by Spring Security before
 * the method body runs.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('SCHOOL_ADMIN')")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public List<UserSummary> list(@RequestParam(defaultValue = "PENDING") UserStatus status) {
        return userAdminService.listByStatus(status);
    }

    @PostMapping("/{id}/approve")
    public UserSummary approve(@PathVariable UUID id) {
        return userAdminService.approve(id);
    }

    @PostMapping("/{id}/reject")
    public UserSummary reject(@PathVariable UUID id) {
        return userAdminService.reject(id);
    }
}
