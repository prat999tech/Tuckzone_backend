package com.school.canteen.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.school.canteen.IntegrationTestBase;
import com.school.canteen.TestDataFactory;
import com.school.canteen.dto.auth.AuthResponse;
import com.school.canteen.dto.auth.LoginRequest;
import com.school.canteen.dto.auth.LogoutRequest;
import com.school.canteen.dto.auth.OtpIssuedResponse;
import com.school.canteen.dto.auth.OtpLoginRequest;
import com.school.canteen.dto.auth.OtpRequest;
import com.school.canteen.dto.auth.ParentRegisterRequest;
import com.school.canteen.dto.auth.PasswordResetRequest;
import com.school.canteen.dto.auth.RefreshRequest;
import com.school.canteen.dto.auth.VerifyEmailRequest;
import com.school.canteen.enums.OtpPurpose;
import com.school.canteen.exception.DuplicateResourceException;
import com.school.canteen.exception.EmailNotVerifiedException;
import com.school.canteen.exception.InvalidCredentialsException;
import com.school.canteen.repository.UserRepository;
import com.school.canteen.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AuthFlowIntegrationTest extends IntegrationTestBase {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Registers a parent and completes email verification, which password sign-in now
     * requires. Most tests care about what happens *after* a usable account exists, so the
     * two-step sign-up is folded away here rather than repeated in each of them.
     */
    private ParentRegisterRequest registerVerifiedParent() {
        ParentRegisterRequest registration = TestDataFactory.parent();
        authService.registerParent(registration);
        OtpIssuedResponse issued = authService.requestOtp(
                new OtpRequest(registration.email(), OtpPurpose.EMAIL_VERIFICATION));
        authService.verifyEmail(new VerifyEmailRequest(registration.email(), issued.devCode()));
        return registration;
    }

    @Test
    @DisplayName("a registered user can sign in with a password once verified")
    void passwordLogin() {
        ParentRegisterRequest registration = registerVerifiedParent();

        AuthResponse response = authService.login(
                new LoginRequest(registration.email(), registration.password()));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.user().email()).isEqualTo(registration.email());
    }

    @Test
    @DisplayName("password sign-in is refused until the emailed code has been entered")
    void passwordLoginBlockedUntilEmailVerified() {
        ParentRegisterRequest registration = TestDataFactory.parent();
        authService.registerParent(registration);

        // Credentials are correct — the account is simply not confirmed yet, and the
        // distinct exception is what lets the app route to the verification screen.
        assertThatThrownBy(() -> authService.login(
                new LoginRequest(registration.email(), registration.password())))
                .isInstanceOf(EmailNotVerifiedException.class);
    }

    @Test
    @DisplayName("signing in with an emailed code also confirms the address")
    void otpLoginVerifiesTheEmail() {
        ParentRegisterRequest registration = TestDataFactory.parent();
        authService.registerParent(registration);

        OtpIssuedResponse issued = authService.requestOtp(
                new OtpRequest(registration.email(), OtpPurpose.LOGIN));
        authService.loginWithOtp(new OtpLoginRequest(registration.email(), issued.devCode()));

        // Reading a code sent to the address proves ownership, so it counts as verification
        // and the user is not left locked out of password sign-in.
        assertThat(userRepository.findByEmail(registration.email()).orElseThrow().isEmailVerified())
                .isTrue();
    }

    @Test
    @DisplayName("email addresses are unique because they identify an account")
    void duplicateEmailRejected() {
        ParentRegisterRequest first = TestDataFactory.parent();
        authService.registerParent(first);

        ParentRegisterRequest second = TestDataFactory.parent();
        ParentRegisterRequest clashing = new ParentRegisterRequest(
                second.fullName(), first.email(), second.mobile(), second.password());

        assertThatThrownBy(() -> authService.registerParent(clashing))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("registration emails a verification code that marks the address verified")
    void registrationSendsVerificationCode() {
        ParentRegisterRequest registration = TestDataFactory.parent();
        authService.registerParent(registration);

        // Registration already issued an EMAIL_VERIFICATION code; request a fresh one so
        // the test has the value (the dev sender echoes it back).
        OtpIssuedResponse issued = authService.requestOtp(
                new OtpRequest(registration.email(), OtpPurpose.EMAIL_VERIFICATION));
        authService.verifyEmail(new VerifyEmailRequest(registration.email(), issued.devCode()));

        assertThat(userRepository.findByEmail(registration.email()).orElseThrow().isEmailVerified())
                .isTrue();
    }

    @Test
    @DisplayName("OTP signs a user in, and the same code cannot be replayed")
    void otpLoginIsSingleUse() {
        ParentRegisterRequest registration = TestDataFactory.parent();
        authService.registerParent(registration);

        OtpIssuedResponse issued = authService.requestOtp(
                new OtpRequest(registration.email(), OtpPurpose.LOGIN));
        assertThat(issued.devCode()).isNotBlank();

        AuthResponse response = authService.loginWithOtp(
                new OtpLoginRequest(registration.email(), issued.devCode()));
        assertThat(response.accessToken()).isNotBlank();

        // Consumed codes stay on record precisely so a captured code cannot be reused.
        assertThatThrownBy(() -> authService.loginWithOtp(
                new OtpLoginRequest(registration.email(), issued.devCode())))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("an OTP issued for login cannot be used to reset a password")
    void otpIsBoundToItsPurpose() {
        ParentRegisterRequest registration = TestDataFactory.parent();
        authService.registerParent(registration);

        OtpIssuedResponse loginCode = authService.requestOtp(
                new OtpRequest(registration.email(), OtpPurpose.LOGIN));

        assertThatThrownBy(() -> authService.resetPassword(new PasswordResetRequest(
                registration.email(), loginCode.devCode(), "BrandNew@2026")))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("requesting an OTP for an unknown address does not reveal that it is unknown")
    void otpRequestDoesNotEnumerateAccounts() {
        OtpIssuedResponse issued = authService.requestOtp(
                new OtpRequest("nobody-here@test.local", OtpPurpose.LOGIN));

        assertThat(issued.message()).contains("If that email address has an account");
        assertThat(issued.devCode()).isNull(); // nothing was actually issued
    }

    @Test
    @DisplayName("logging out revokes the refresh token server-side")
    void logoutRevokesSession() {
        ParentRegisterRequest registration = registerVerifiedParent();
        AuthResponse session = authService.login(
                new LoginRequest(registration.email(), registration.password()));

        // Still usable before logout.
        assertThat(authService.refresh(new RefreshRequest(session.refreshToken())).accessToken())
                .isNotBlank();

        authService.logout(new LogoutRequest(session.refreshToken()));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(session.refreshToken())))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("changing a password ends every existing session")
    void passwordResetRevokesAllSessions() {
        ParentRegisterRequest registration = registerVerifiedParent();
        AuthResponse session = authService.login(
                new LoginRequest(registration.email(), registration.password()));

        OtpIssuedResponse resetCode = authService.requestOtp(
                new OtpRequest(registration.email(), OtpPurpose.PASSWORD_RESET));
        authService.resetPassword(new PasswordResetRequest(
                registration.email(), resetCode.devCode(), "BrandNew@2026"));

        // If the account had been taken over, the attacker's session must not survive.
        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(session.refreshToken())))
                .isInstanceOf(InvalidCredentialsException.class);

        // The old password must no longer work, the new one must.
        assertThatThrownBy(() -> authService.login(
                new LoginRequest(registration.email(), registration.password())))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(authService.login(new LoginRequest(registration.email(), "BrandNew@2026"))
                .accessToken()).isNotBlank();
    }
}
