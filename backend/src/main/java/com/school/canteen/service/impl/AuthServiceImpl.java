package com.school.canteen.service.impl;

import com.school.canteen.dto.UserSummary;
import com.school.canteen.dto.auth.AuthResponse;
import com.school.canteen.dto.auth.LoginRequest;
import com.school.canteen.dto.auth.ParentRegisterRequest;
import com.school.canteen.dto.auth.RefreshRequest;
import com.school.canteen.dto.auth.StudentRegisterRequest;
import com.school.canteen.dto.auth.TeacherRegisterRequest;
import com.school.canteen.entity.StudentProfile;
import com.school.canteen.entity.TeacherProfile;
import com.school.canteen.entity.User;
import com.school.canteen.enums.Role;
import com.school.canteen.enums.UserStatus;
import com.school.canteen.exception.AccountNotActiveException;
import com.school.canteen.exception.DuplicateResourceException;
import com.school.canteen.exception.InvalidCredentialsException;
import com.school.canteen.mapper.UserMapper;
import com.school.canteen.repository.StudentProfileRepository;
import com.school.canteen.repository.TeacherProfileRepository;
import com.school.canteen.repository.UserRepository;
import com.school.canteen.security.JwtService;
import com.school.canteen.service.AuthService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthServiceImpl(UserRepository userRepository,
                           StudentProfileRepository studentProfileRepository,
                           TeacherProfileRepository teacherProfileRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           UserMapper userMapper) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
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
        profile.setParentMobile(request.parentMobile());
        profile.setStudentMobile(request.studentMobile());
        studentProfileRepository.save(profile);

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

        return userMapper.toSummary(user);
    }

    @Override
    @Transactional
    public UserSummary registerParent(ParentRegisterRequest request) {
        requireEmailAvailable(request.email());
        User user = newUser(request.fullName(), request.email(), request.mobile(),
                request.password(), Role.PARENT);
        userRepository.save(user);
        return userMapper.toSummary(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        ensureApproved(user);
        return buildAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
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

        User user = userRepository.findByEmail(claims.getSubject())
                .orElseThrow(InvalidCredentialsException::new);
        ensureApproved(user);
        return buildAuthResponse(user);
    }

    // --- helpers ---------------------------------------------------------------

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
        user.setStatus(UserStatus.APPROVED); // active immediately upon registration
        return user;
    }

    private void ensureApproved(User user) {
        switch (user.getStatus()) {
            case APPROVED -> {
                // ok
            }
            case PENDING -> throw new AccountNotActiveException(
                    "Your account is awaiting administrator approval");
            case REJECTED -> throw new AccountNotActiveException(
                    "Your registration was rejected");
            case DISABLED -> throw new AccountNotActiveException(
                    "Your account has been disabled");
        }
    }

    private AuthResponse buildAuthResponse(User user) {
        String role = user.getRole().name();
        String accessToken = jwtService.generateAccessToken(user.getEmail(), role);
        String refreshToken = jwtService.generateRefreshToken(user.getEmail(), role);
        return new AuthResponse(
                TOKEN_TYPE,
                accessToken,
                refreshToken,
                jwtService.accessTtlSeconds(),
                userMapper.toSummary(user));
    }
}
