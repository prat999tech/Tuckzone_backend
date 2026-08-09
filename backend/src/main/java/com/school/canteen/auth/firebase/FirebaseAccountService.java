package com.school.canteen.auth.firebase;

import com.school.canteen.dto.auth.AuthResponse;
import com.school.canteen.dto.auth.FirebaseParentRegisterRequest;
import com.school.canteen.dto.auth.FirebaseStudentRegisterRequest;
import com.school.canteen.dto.auth.FirebaseTeacherRegisterRequest;
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
import com.school.canteen.exception.FirebaseUserNotRegisteredException;
import com.school.canteen.repository.StudentProfileRepository;
import com.school.canteen.repository.TeacherProfileRepository;
import com.school.canteen.repository.UserRepository;
import com.school.canteen.security.SessionIssuer;
import com.school.canteen.service.NotificationService;
import com.school.canteen.service.OtpService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bridges a Firebase-verified identity to an application {@link User} — the only place that
 * decides "login vs. auto-link vs. needs registration". Everything about how a session is
 * actually issued or how a role's profile is stored is delegated to the same machinery the
 * password/OTP flows use ({@link SessionIssuer}, the profile repositories), so this class
 * adds a new front door without duplicating what happens behind it.
 */
@Service
public class FirebaseAccountService {

    private final FirebaseAuthService firebaseAuthService;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final SessionIssuer sessionIssuer;
    private final OtpService otpService;
    private final NotificationService notificationService;

    public FirebaseAccountService(FirebaseAuthService firebaseAuthService,
                                  UserRepository userRepository,
                                  StudentProfileRepository studentProfileRepository,
                                  TeacherProfileRepository teacherProfileRepository,
                                  SessionIssuer sessionIssuer,
                                  OtpService otpService,
                                  NotificationService notificationService) {
        this.firebaseAuthService = firebaseAuthService;
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.sessionIssuer = sessionIssuer;
        this.otpService = otpService;
        this.notificationService = notificationService;
    }

    /**
     * The sign-in path: verifies the token, then either logs in (already linked), silently
     * links (a verified Firebase email/phone matches exactly one existing account — no
     * password reset or manual step required), or reports that registration is still
     * needed.
     */
    @Transactional
    public AuthResponse exchange(String idToken) {
        FirebaseVerifiedIdentity identity = firebaseAuthService.verify(idToken);

        User user = userRepository.findByFirebaseUid(identity.uid())
                .orElseGet(() -> autoLink(identity).orElseThrow(FirebaseUserNotRegisteredException::new));

        ensureActive(user);
        notifySignIn(user);
        return sessionIssuer.issue(user);
    }

    @Transactional
    public AuthResponse registerStudent(FirebaseStudentRegisterRequest request) {
        FirebaseVerifiedIdentity identity = verifyForRegistration(request.idToken());
        requireEmailAvailable(request.email());
        if (studentProfileRepository.existsByAdmissionNumber(request.admissionNumber())) {
            throw new DuplicateResourceException("Admission number already registered");
        }

        User user = newFirebaseUser(identity, request.fullName(), request.email(), request.mobile(), Role.STUDENT);
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

        sendEmailVerificationIfNeeded(user);
        return sessionIssuer.issue(user);
    }

    @Transactional
    public AuthResponse registerTeacher(FirebaseTeacherRegisterRequest request) {
        FirebaseVerifiedIdentity identity = verifyForRegistration(request.idToken());
        requireEmailAvailable(request.email());
        if (teacherProfileRepository.existsByEmployeeId(request.employeeId())) {
            throw new DuplicateResourceException("Employee ID already registered");
        }

        User user = newFirebaseUser(identity, request.fullName(), request.email(), request.mobile(), Role.TEACHER);
        userRepository.save(user);

        TeacherProfile profile = new TeacherProfile();
        profile.setUser(user);
        profile.setEmployeeId(request.employeeId());
        profile.setDepartment(request.department());
        teacherProfileRepository.save(profile);

        sendEmailVerificationIfNeeded(user);
        return sessionIssuer.issue(user);
    }

    @Transactional
    public AuthResponse registerParent(FirebaseParentRegisterRequest request) {
        FirebaseVerifiedIdentity identity = verifyForRegistration(request.idToken());
        requireEmailAvailable(request.email());

        User user = newFirebaseUser(identity, request.fullName(), request.email(), request.mobile(), Role.PARENT);
        userRepository.save(user);

        sendEmailVerificationIfNeeded(user);
        return sessionIssuer.issue(user);
    }

    // --- helpers ---------------------------------------------------------------

    /**
     * Verifies the token and rejects outright if this Firebase identity is already linked
     * to an account — otherwise the same verified session could be replayed to create a
     * second, orphaned registration.
     */
    private FirebaseVerifiedIdentity verifyForRegistration(String idToken) {
        FirebaseVerifiedIdentity identity = firebaseAuthService.verify(idToken);
        if (userRepository.findByFirebaseUid(identity.uid()).isPresent()) {
            throw new DuplicateResourceException("This Firebase identity is already registered");
        }
        return identity;
    }

    /**
     * Matches a newly-verified Firebase identity against an existing password-registered
     * account, so an existing user's first Firebase sign-in links automatically rather than
     * requiring a password reset. Only links when the match is unambiguous:
     *  - email sign-in: exact match on the (unique) email column.
     *  - phone sign-in: exactly one existing user has that mobile number — mobile has no
     *    uniqueness constraint, so two or more matches is treated as "can't safely link"
     *    rather than guessing.
     * A candidate already linked to a *different* Firebase identity is never touched.
     */
    private Optional<User> autoLink(FirebaseVerifiedIdentity identity) {
        Optional<User> candidate = identity.email() != null
                ? userRepository.findByEmail(identity.email())
                : matchByPhone(identity.phoneNumber());

        return candidate.filter(user -> user.getFirebaseUid() == null)
                .map(user -> {
                    user.setFirebaseUid(identity.uid());
                    if (identity.emailVerified()) {
                        user.setEmailVerified(true);
                    }
                    return user;
                });
    }

    private Optional<User> matchByPhone(String e164Phone) {
        if (e164Phone == null) {
            return Optional.empty();
        }
        String localMobile = toLocalMobile(e164Phone);
        if (localMobile == null) {
            return Optional.empty();
        }
        List<User> matches = userRepository.findAllByMobile(localMobile);
        return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
    }

    /** Firebase phone numbers are E.164 (+91XXXXXXXXXX); our column stores the bare
     *  10-digit Indian number. Anything not matching that shape can't be confidently
     *  normalised, so it's left unmatched rather than guessed at. */
    private String toLocalMobile(String e164Phone) {
        String withoutCountryCode = e164Phone.startsWith("+91") ? e164Phone.substring(3) : e164Phone;
        return withoutCountryCode.matches("^[6-9]\\d{9}$") ? withoutCountryCode : null;
    }

    private User newFirebaseUser(FirebaseVerifiedIdentity identity, String fullName, String email,
                                 String mobile, Role role) {
        boolean emailIsFirebaseVerified = identity.email() != null;
        if (emailIsFirebaseVerified && !identity.email().equalsIgnoreCase(email)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Email does not match the address verified with Firebase");
        }

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setMobile(mobile);
        user.setPasswordHash(null); // Firebase verifies this identity; no local secret to check.
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setFirebaseUid(identity.uid());
        // Firebase already verified this exact address (email sign-in); a phone sign-up's
        // email is just contact info until it goes through the same verification everyone
        // else does.
        user.setEmailVerified(emailIsFirebaseVerified && identity.emailVerified());
        return user;
    }

    /** A phone sign-up's email hasn't been verified by anyone yet — send the same code the
     *  password flow does, so it can be confirmed through the existing /verify-email step. */
    private void sendEmailVerificationIfNeeded(User user) {
        if (!user.isEmailVerified()) {
            otpService.issue(user.getEmail(), user.getFullName(), OtpPurpose.EMAIL_VERIFICATION);
        }
    }

    private void requireEmailAvailable(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already registered");
        }
    }

    private void ensureActive(User user) {
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new AccountNotActiveException("Your account has been disabled");
        }
    }

    private void notifySignIn(User user) {
        notificationService.notifyUser(user, NotificationEvent.SIGNED_IN,
                "New sign-in to your account",
                "Your TuckZone account was just signed in to using Firebase authentication. "
                        + "If this was not you, contact the canteen.",
                Map.of("method", "firebase"));
    }
}
