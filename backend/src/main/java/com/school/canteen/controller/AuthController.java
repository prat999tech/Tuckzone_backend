package com.school.canteen.controller;

import com.school.canteen.dto.UserSummary;
import com.school.canteen.dto.auth.AuthResponse;
import com.school.canteen.dto.auth.LoginRequest;
import com.school.canteen.dto.auth.ParentRegisterRequest;
import com.school.canteen.dto.auth.RefreshRequest;
import com.school.canteen.dto.auth.StudentRegisterRequest;
import com.school.canteen.dto.auth.TeacherRegisterRequest;
import com.school.canteen.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public authentication endpoints. @Valid triggers bean validation on the request body
 * before the method runs; failures are turned into 400s by the global handler.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register/student")
    @ResponseStatus(HttpStatus.CREATED)
    public UserSummary registerStudent(@Valid @RequestBody StudentRegisterRequest request) {
        return authService.registerStudent(request);
    }

    @PostMapping("/register/teacher")
    @ResponseStatus(HttpStatus.CREATED)
    public UserSummary registerTeacher(@Valid @RequestBody TeacherRegisterRequest request) {
        return authService.registerTeacher(request);
    }

    @PostMapping("/register/parent")
    @ResponseStatus(HttpStatus.CREATED)
    public UserSummary registerParent(@Valid @RequestBody ParentRegisterRequest request) {
        return authService.registerParent(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }
}
