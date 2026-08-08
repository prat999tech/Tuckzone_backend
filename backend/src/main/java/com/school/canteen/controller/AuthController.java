package com.school.canteen.controller;

import com.school.canteen.auth.firebase.FirebaseAccountService;
import com.school.canteen.dto.UserSummary;
import com.school.canteen.dto.auth.AdminRegisterRequest;
import com.school.canteen.dto.auth.AuthResponse;
import com.school.canteen.dto.auth.FirebaseExchangeRequest;
import com.school.canteen.dto.auth.FirebaseParentRegisterRequest;
import com.school.canteen.dto.auth.FirebaseStudentRegisterRequest;
import com.school.canteen.dto.auth.FirebaseTeacherRegisterRequest;
import com.school.canteen.dto.auth.LoginRequest;
import com.school.canteen.dto.auth.LogoutRequest;
import com.school.canteen.dto.auth.OtpIssuedResponse;
import com.school.canteen.dto.auth.OtpLoginRequest;
import com.school.canteen.dto.auth.OtpRequest;
import com.school.canteen.dto.auth.ParentRegisterRequest;
import com.school.canteen.dto.auth.PasswordResetRequest;
import com.school.canteen.dto.auth.RefreshRequest;
import com.school.canteen.dto.auth.VerifyEmailRequest;
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
 *
 * The {@code /firebase/*} endpoints are a second, additive front door alongside the
 * password/OTP one above — see {@link FirebaseAccountService} for what "additive" means in
 * practice (existing accounts are never touched unless they choose to link).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final FirebaseAccountService firebaseAccountService;

    public AuthController(AuthService authService, FirebaseAccountService firebaseAccountService) {
        this.authService = authService;
        this.firebaseAccountService = firebaseAccountService;
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

    /**
     * Canteen-admin sign-up. Public like the others, but gated by the invite code so it
     * cannot be used to take over the canteen.
     */
    @PostMapping("/register/admin")
    @ResponseStatus(HttpStatus.CREATED)
    public UserSummary registerAdmin(@Valid @RequestBody AdminRegisterRequest request) {
        return authService.registerAdmin(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Emails a one-time passcode for sign-in or password reset. */
    @PostMapping("/otp/request")
    public OtpIssuedResponse requestOtp(@Valid @RequestBody OtpRequest request) {
        return authService.requestOtp(request);
    }

    /** Signs in with email + passcode, no password required. */
    @PostMapping("/otp/login")
    public AuthResponse loginWithOtp(@Valid @RequestBody OtpLoginRequest request) {
        return authService.loginWithOtp(request);
    }

    /** Confirms the email address supplied at registration, using the code emailed to it. */
    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
    }

    /**
     * Sets a new password after verifying a passcode emailed to the account.
     * Covers both "forgot password" and a deliberate change, and ends all sessions.
     */
    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    /**
     * Ends this device's session server-side. Clearing tokens on the client alone leaves
     * the refresh token usable by anyone who captured it.
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
    }

    /**
     * Sign-in with a Firebase ID token (phone OTP or email, verified client-side by
     * Firebase first). Logs in if this identity is already linked, silently links it to a
     * matching existing account, or responds 404/{@code FIREBASE_USER_NOT_REGISTERED} if
     * the client should show a "complete registration" screen instead.
     */
    @PostMapping("/firebase/exchange")
    public AuthResponse firebaseExchange(@Valid @RequestBody FirebaseExchangeRequest request) {
        return firebaseAccountService.exchange(request.idToken());
    }

    @PostMapping("/firebase/register/student")
    public AuthResponse firebaseRegisterStudent(@Valid @RequestBody FirebaseStudentRegisterRequest request) {
        return firebaseAccountService.registerStudent(request);
    }

    @PostMapping("/firebase/register/teacher")
    public AuthResponse firebaseRegisterTeacher(@Valid @RequestBody FirebaseTeacherRegisterRequest request) {
        return firebaseAccountService.registerTeacher(request);
    }

    @PostMapping("/firebase/register/parent")
    public AuthResponse firebaseRegisterParent(@Valid @RequestBody FirebaseParentRegisterRequest request) {
        return firebaseAccountService.registerParent(request);
    }
}
