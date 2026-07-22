package com.school.canteen.controller;

import com.school.canteen.dto.parent.ChildResponse;
import com.school.canteen.dto.parent.LinkChildRequest;
import com.school.canteen.security.AppUserDetails;
import com.school.canteen.service.ParentChildService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** A parent's linked children. Restricted to the PARENT role; acts on the caller only. */
@RestController
@RequestMapping("/api/parent/children")
@PreAuthorize("hasRole('PARENT')")
public class ParentChildController {

    private final ParentChildService parentChildService;

    public ParentChildController(ParentChildService parentChildService) {
        this.parentChildService = parentChildService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChildResponse link(@AuthenticationPrincipal AppUserDetails principal,
                              @Valid @RequestBody LinkChildRequest request) {
        return parentChildService.linkChild(principal.getUser().getId(), request);
    }

    @GetMapping
    public List<ChildResponse> children(@AuthenticationPrincipal AppUserDetails principal) {
        return parentChildService.listChildren(principal.getUser().getId());
    }

    @DeleteMapping("/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlink(@AuthenticationPrincipal AppUserDetails principal,
                       @PathVariable UUID linkId) {
        parentChildService.unlink(principal.getUser().getId(), linkId);
    }
}
