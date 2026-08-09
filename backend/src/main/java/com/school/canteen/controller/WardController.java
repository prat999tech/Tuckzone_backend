package com.school.canteen.controller;

import com.school.canteen.dto.ward.WardRequest;
import com.school.canteen.dto.ward.WardResponse;
import com.school.canteen.security.AppUserDetails;
import com.school.canteen.service.WardService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** A parent's wards. Restricted to the PARENT role; acts on the caller's own wards only. */
@RestController
@RequestMapping("/api/parent/wards")
@PreAuthorize("hasRole('PARENT')")
public class WardController {

    private final WardService wardService;

    public WardController(WardService wardService) {
        this.wardService = wardService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WardResponse create(@AuthenticationPrincipal AppUserDetails principal,
                                @Valid @RequestBody WardRequest request) {
        return wardService.create(principal.getUser().getId(), request);
    }

    @GetMapping
    public List<WardResponse> list(@AuthenticationPrincipal AppUserDetails principal) {
        return wardService.list(principal.getUser().getId());
    }

    @PutMapping("/{wardId}")
    public WardResponse update(@AuthenticationPrincipal AppUserDetails principal,
                                @PathVariable UUID wardId,
                                @Valid @RequestBody WardRequest request) {
        return wardService.update(principal.getUser().getId(), wardId, request);
    }

    @DeleteMapping("/{wardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AppUserDetails principal,
                        @PathVariable UUID wardId) {
        wardService.delete(principal.getUser().getId(), wardId);
    }
}
