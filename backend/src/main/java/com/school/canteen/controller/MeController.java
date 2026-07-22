package com.school.canteen.controller;

import com.school.canteen.dto.UserSummary;
import com.school.canteen.mapper.UserMapper;
import com.school.canteen.security.AppUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A protected endpoint used to confirm a token works. @AuthenticationPrincipal injects the
 * principal our JWT filter placed in the SecurityContext for this request.
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserMapper userMapper;

    public MeController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @GetMapping
    public UserSummary me(@AuthenticationPrincipal AppUserDetails principal) {
        return userMapper.toSummary(principal.getUser());
    }
}
