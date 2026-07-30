package com.school.canteen.controller;

import com.school.canteen.dto.UserSummary;
import com.school.canteen.dto.profile.ProfileUpdateRequest;
import com.school.canteen.security.AppUserDetails;
import com.school.canteen.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The signed-in user's own profile. @AuthenticationPrincipal injects the principal the JWT
 * filter placed in the SecurityContext, so these endpoints can only ever act on the caller.
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final ProfileService profileService;

    public MeController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public UserSummary me(@AuthenticationPrincipal AppUserDetails principal) {
        return profileService.getProfile(principal.getUser().getId());
    }

    @PutMapping
    public UserSummary updateProfile(@AuthenticationPrincipal AppUserDetails principal,
                                     @Valid @RequestBody ProfileUpdateRequest request) {
        return profileService.updateProfile(principal.getUser().getId(), request);
    }
}
