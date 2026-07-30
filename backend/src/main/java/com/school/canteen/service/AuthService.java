package com.school.canteen.service;

import com.school.canteen.dto.UserSummary;
import com.school.canteen.dto.auth.AdminRegisterRequest;
import com.school.canteen.dto.auth.AuthResponse;
import com.school.canteen.dto.auth.LoginRequest;
import com.school.canteen.dto.auth.LogoutRequest;
import com.school.canteen.dto.auth.OtpIssuedResponse;
import com.school.canteen.dto.auth.OtpLoginRequest;
import com.school.canteen.dto.auth.OtpRequest;
import com.school.canteen.dto.auth.ParentRegisterRequest;
import com.school.canteen.dto.auth.PasswordResetRequest;
import com.school.canteen.dto.auth.RefreshRequest;
import com.school.canteen.dto.auth.StudentRegisterRequest;
import com.school.canteen.dto.auth.TeacherRegisterRequest;
import com.school.canteen.dto.auth.VerifyEmailRequest;

/**
 * Registration + authentication use cases. Controllers depend on this interface, not the
 * implementation (Dependency Inversion) — the wiring is swappable and easily mocked.
 */
public interface AuthService {

    UserSummary registerStudent(StudentRegisterRequest request);

    UserSummary registerTeacher(TeacherRegisterRequest request);

    UserSummary registerParent(ParentRegisterRequest request);

    /** Canteen-admin sign-up; requires the configured invite code. */
    UserSummary registerAdmin(AdminRegisterRequest request);

    /** Sends a passcode for sign-in or password reset. */
    OtpIssuedResponse requestOtp(OtpRequest request);

    /** Signs in with mobile + passcode instead of a password. */
    AuthResponse loginWithOtp(OtpLoginRequest request);

    /** Sets a new password after verifying a passcode, then ends all existing sessions. */
    void resetPassword(PasswordResetRequest request);

    /** Confirms the address supplied at registration using the emailed code. */
    void verifyEmail(VerifyEmailRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshRequest request);

    /** Revokes the presented refresh token so this device's session cannot be resumed. */
    void logout(LogoutRequest request);
}
