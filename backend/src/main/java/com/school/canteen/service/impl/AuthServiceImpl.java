package com.school.canteen.service.impl;

import com.school.canteen.dto.UserSummary;
import com.school.canteen.dto.auth.AuthResponse;
import com.school.canteen.config.AdminProperties;
import com.school.canteen.config.OtpProperties;
import com.school.canteen.config.RateLimitProperties;
import com.school.canteen.dto.auth.AdminRegisterRequest;
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
import com.school.canteen.entity.RefreshToken;
import com.school.canteen.entity.StudentProfile;
import com.school.canteen.entity.TeacherProfile;
import com.school.canteen.entity.User;
import com.school.canteen.enums.NotificationEvent;
import com.school.canteen.enums.OtpPurpose;
import com.school.canteen.enums.Role;
import com.school.canteen.enums.UserStatus;
import com.school.canteen.exception.AccountNotActiveException;
import com.school.canteen.exception.ApiException;
import com.school.canteen.exception.DuplicateResourceException;
import com.school.canteen.exception.EmailNotVerifiedException;
import com.school.canteen.exception.InvalidCredentialsException;
import com.school.canteen.mapper.UserMapper;
import com.school.canteen.repository.RefreshTokenRepository;
import com.school.canteen.repository.StudentProfileRepository;
import com.school.canteen.repository.TeacherProfileRepository;
import com.school.canteen.repository.UserRepository;
import com.school.canteen.security.JwtService;
import com.school.canteen.security.RateLimiterService;
import com.school.canteen.security.SessionIssuer;
import com.school.canteen.service.AuthService;
import com.school.canteen.service.NotificationService;
import com.school.canteen.service.OtpService;
import com.school.canteen.util.TokenHasher;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final RateLimiterService rateLimiter;
    private final RateLimitProperties rateLimitProperties;
    private final OtpService otpService;
    private final OtpProperties otpProperties;
    private final AdminProperties adminProperties;
    private final NotificationService notificationService;
    private final SessionIssuer sessionIssuer;

    public AuthServiceImpl(UserRepository userRepository,
                           StudentProfileRepository studentProfileRepository,
                           TeacherProfileRepository teacherProfileRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           UserMapper userMapper,
                           RateLimiterService rateLimiter,
                           RateLimitProperties rateLimitProperties,
                           OtpService otpService,
                           OtpProperties otpProperties,
                           AdminProperties adminProperties,
                           NotificationService notificationService,
                           SessionIssuer sessionIssuer) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.rateLimiter = rateLimiter;
        this.rateLimitProperties = rateLimitProperties;
        this.otpService = otpService;
        this.otpProperties = otpProperties;
        this.adminProperties = adminProperties;
        this.notificationService = notificationService;
        this.sessionIssuer = sessionIssuer;
    }

    @Override
    @Transactional
    public UserSummary registerStudent(StudentRegisterRequest request) {
        requireEmailAvailable(request.email());
        if (studentProfileRepository.existsByAdmissionNumber(request.admissionNumber())) {
            throw new DuplicateResourceException("Admission number already registered");
        }

        User user = newUser(request.fullName(), request.email(), request.mobile(),
                request.password(), Role.STUDENT);
        userRepository.save(user);

        StudentProfile profile = new StudentProfile();
        profile.setUser(user);
        profile.setAdmissionNumber(request.admissionNumber());
        profile.setStudentClass(request.studentClass());
        profile.setSection(request.section());
        profile.setRollNumber(request.rollNumber());
        profile.setSeatNumber(request.seatNumber());
        profile.setParentMobile(request.parentMobile());
        profile.setStudentMobile(request.studentMobile());
        studentProfileRepository.save(profile);

        sendEmailVerification(user);
        return userMapper.toSummary(user);
    }

    @Override
    @Transactional
    public UserSummary registerTeacher(TeacherRegisterRequest request) {
        requireEmailAvailable(request.email());
        if (teacherProfileRepository.existsByEmployeeId(request.employeeId())) {
            throw new DuplicateResourceException("Employee ID already registered");
        }

        User user = newUser(request.fullName(), request.email(), request.mobile(),
                request.password(), Role.TEACHER);
        userRepository.save(user);

        TeacherProfile profile = new TeacherProfile();
        profile.setUser(user);
        profile.setEmployeeId(request.employeeId());
        profile.setDepartment(request.department());
        teacherProfileRepository.save(profile);

        sendEmailVerification(user);
        return userMapper.toSummary(user);
    }

    @Override
    @Transactional
    public UserSummary registerParent(ParentRegisterRequest request) {
        requireEmailAvailable(request.email());
        User user = newUser(request.fullName(), request.email(), request.mobile(),
                request.password(), Role.PARENT);
        userRepository.save(user);
        sendEmailVerification(user);
        return userMapper.toSummary(user);
    }

    @Override
    @Transactional
    public UserSummary registerAdmin(AdminRegisterRequest request) {
        // Constant-time comparison so the invite code cannot be discovered by timing how
        // long a wrong guess takes.
        boolean codeMatches = MessageDigest.isEqual(
                request.signupCode().getBytes(StandardCharsets.UTF_8),
                adminProperties.signupCode().getBytes(StandardCharsets.UTF_8));
        if (!codeMatches) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Invalid admin signup code");
        }
        requireEmailAvailable(request.email());

        User user = newUser(request.fullName(), request.email(), request.mobile(),
                request.password(), Role.CANTEEN_ADMIN);
        userRepository.save(user);
        sendEmailVerification(user);
        return userMapper.toSummary(user);
    }

    @Override
    @Transactional
    public OtpIssuedResponse requestOtp(OtpRequest request) {
        // Always answer the same way, whether or not the address is registered. Saying
        // "no such account" would turn this endpoint into a way to discover who has one.
        User user = userRepository.findByEmail(request.email()).orElse(null);
        String devCode = (user == null)
                ? null
                : otpService.issue(user.getEmail(), user.getFullName(), request.purpose());

        return new OtpIssuedResponse(
                "If that email address has an account, a passcode has been sent to it.",
                otpProperties.ttlMinutes(),
                devCode,
                otpProperties.resendCooldownSeconds());
    }

    @Override
    @Transactional
    public AuthResponse loginWithOtp(OtpLoginRequest request) {
        otpService.verify(request.email(), request.code(), OtpPurpose.LOGIN);

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);
        ensureActive(user);
        // Deliberately NOT gated on emailVerified: reading a code sent to that address is
        // itself proof of ownership, so a successful passcode sign-in confirms the address
        // rather than being blocked by it. This also keeps people from locking themselves
        // out if the registration email was missed.
        user.setEmailVerified(true);
        notifySignIn(user, "one-time passcode");
        return sessionIssuer.issue(user);
    }

    @Override
    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        otpService.verify(request.email(), request.code(), OtpPurpose.PASSWORD_RESET);

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));

        // Changing a password must end every existing session: if the account was taken
        // over, the attacker's refresh token would otherwise survive the reset.
        refreshTokenRepository.revokeAllForUser(user.getId(), Instant.now());

        // Tell the owner their password changed. If it was not them, this is the signal
        // that lets them react before the account is misused.
        notificationService.notifyUser(user, NotificationEvent.PASSWORD_CHANGED,
                "Password changed",
                "Your TuckZone password was changed. If this was not you, contact the canteen.",
                Map.of());
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Throttle per email so passwords cannot be attacked at network speed.
        String rateKey = "login:" + request.email().toLowerCase(Locale.ROOT);
        rateLimiter.checkAndConsume(rateKey,
                rateLimitProperties.loginAttemptsPerWindow(),
                Duration.ofMinutes(rateLimitProperties.windowMinutes()),
                "Too many sign-in attempts. Please try again later.");

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        // A Firebase-only account (phone or email sign-in, never set a local password) has
        // no hash to compare against — reject cleanly rather than letting BCrypt's null
        // check surface as an unhandled 500.
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        ensureActive(user);
        // Enforced only for password sign-in. Until this check existed the code emailed at
        // registration was decorative: nothing ever required it to be entered.
        ensureEmailVerified(user);
        rateLimiter.reset(rateKey); // successful sign-in clears the counter
        notifySignIn(user, "password");
        return sessionIssuer.issue(user);
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        Claims claims;
        try {
            claims = jwtService.parse(request.refreshToken());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidCredentialsException();
        }
        if (!jwtService.isRefreshToken(claims)) {
            throw new InvalidCredentialsException();
        }

        // A valid signature is no longer sufficient: the token must also still be an
        // active server-side session. Without this check a leaked refresh token stayed
        // usable for its full 7-day life with no way to cut it off.
        RefreshToken stored = refreshTokenRepository
                .findByTokenHash(TokenHasher.sha256Hex(request.refreshToken()))
                .orElseThrow(InvalidCredentialsException::new);
        if (!stored.isUsable(Instant.now())) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByEmail(claims.getSubject())
                .orElseThrow(InvalidCredentialsException::new);
        ensureActive(user);

        // Issue a fresh access token but keep the existing refresh session. Rotating the
        // refresh token here would break clients that fire two requests in parallel: both
        // would try to refresh, and the loser would be logged out.
        return new AuthResponse(
                TOKEN_TYPE,
                jwtService.generateAccessToken(user.getEmail(), user.getRole().name()),
                request.refreshToken(),
                jwtService.accessTtlSeconds(),
                userMapper.toSummary(user));
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        // Idempotent by design: an unknown or already-revoked token is not an error,
        // because the caller's intent (this session must not work) is already satisfied.
        refreshTokenRepository.findByTokenHash(TokenHasher.sha256Hex(request.refreshToken()))
                .filter(token -> token.getRevokedAt() == null)
                .ifPresent(token -> token.setRevokedAt(Instant.now()));
    }

    // --- helpers ---------------------------------------------------------------

    @Override
    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        otpService.verify(request.email(), request.code(), OtpPurpose.EMAIL_VERIFICATION);

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);
        user.setEmailVerified(true);
    }

    /**
     * Emails the account owner that their account was just signed in to.
     *
     * This is the main value of an alert: if it was not them, it is the earliest signal
     * they get that someone else has their credentials.
     */
    private void notifySignIn(User user, String method) {
        notificationService.notifyUser(user, NotificationEvent.SIGNED_IN,
                "New sign-in to your account",
                "Your TuckZone account was just signed in to using your " + method
                        + ". If this was not you, reset your password immediately.",
                Map.of("method", method));
    }

    /**
     * Sends the code that confirms the address supplied at registration is real.
     *
     * Deliberately does not block sign-in: locking a parent out of ordering lunch because
     * a verification email landed in spam would be a worse failure than an unverified
     * address. The flag is recorded so it can be enforced later if needed.
     */
    private void sendEmailVerification(User user) {
        otpService.issue(user.getEmail(), user.getFullName(), OtpPurpose.EMAIL_VERIFICATION);
    }

    private void requireEmailAvailable(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already registered");
        }
    }

    private User newUser(String fullName, String email, String mobile, String rawPassword,
                         Role role) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setMobile(mobile);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        // No approval queue: the canteen is vendor-run and a parent's claim on a child is
        // verified by admission number + recorded parent mobile, not by an administrator.
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private void ensureActive(User user) {
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new AccountNotActiveException("Your account has been disabled");
        }
    }

    private void ensureEmailVerified(User user) {
        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException(user.getEmail());
        }
    }

    /** Removes long-expired session rows so the table does not grow without bound. */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeExpiredRefreshTokens() {
        refreshTokenRepository.deleteExpiredBefore(Instant.now().minus(Duration.ofDays(30)));
    }
}
