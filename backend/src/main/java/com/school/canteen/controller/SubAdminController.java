package com.school.canteen.controller;

import com.school.canteen.dto.UserSummary;
import com.school.canteen.dto.subadmin.SubAdminCreateRequest;
import com.school.canteen.dto.subadmin.SubAdminUpdateRequest;
import com.school.canteen.service.SubAdminService;
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

/**
 * Sub Admin account administration. CANTEEN_ADMIN-only, deliberately not opened up to
 * SUB_ADMIN via hasAnyRole — a Sub Admin must never be able to create, edit or delete any
 * admin account, including another Sub Admin's.
 */
@RestController
@RequestMapping("/api/admin/sub-admins")
@PreAuthorize("hasRole('CANTEEN_ADMIN')")
public class SubAdminController {

    private final SubAdminService subAdminService;

    public SubAdminController(SubAdminService subAdminService) {
        this.subAdminService = subAdminService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserSummary create(@Valid @RequestBody SubAdminCreateRequest request) {
        return subAdminService.create(request);
    }

    @GetMapping
    public List<UserSummary> list(@RequestParam(required = false) Integer page,
                                  @RequestParam(required = false) Integer size) {
        return subAdminService.list(page, size);
    }

    @GetMapping("/{id}")
    public UserSummary get(@PathVariable UUID id) {
        return subAdminService.get(id);
    }

    @PutMapping("/{id}")
    public UserSummary update(@PathVariable UUID id,
                              @Valid @RequestBody SubAdminUpdateRequest request) {
        return subAdminService.update(id, request);
    }

    @PostMapping("/{id}/deactivate")
    public UserSummary deactivate(@PathVariable UUID id) {
        return subAdminService.deactivate(id);
    }

    @PostMapping("/{id}/activate")
    public UserSummary activate(@PathVariable UUID id) {
        return subAdminService.activate(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        subAdminService.delete(id);
    }
}
