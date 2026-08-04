package com.school.canteen.service.impl;

import com.school.canteen.dto.UserSummary;
import com.school.canteen.dto.subadmin.SubAdminCreateRequest;
import com.school.canteen.dto.subadmin.SubAdminUpdateRequest;
import com.school.canteen.entity.User;
import com.school.canteen.enums.Role;
import com.school.canteen.enums.UserStatus;
import com.school.canteen.exception.DuplicateResourceException;
import com.school.canteen.exception.ResourceNotFoundException;
import com.school.canteen.mapper.UserMapper;
import com.school.canteen.repository.RefreshTokenRepository;
import com.school.canteen.repository.UserRepository;
import com.school.canteen.service.SubAdminService;
import com.school.canteen.util.PageRequests;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubAdminServiceImpl implements SubAdminService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public SubAdminServiceImpl(UserRepository userRepository,
                               RefreshTokenRepository refreshTokenRepository,
                               PasswordEncoder passwordEncoder,
                               UserMapper userMapper) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public UserSummary create(SubAdminCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered");
        }

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setMobile(request.mobile());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.SUB_ADMIN);
        user.setStatus(UserStatus.ACTIVE);
        // The Super Admin is vouching for this account by creating it directly, so there is
        // no email-ownership loop to run — unlike self-registration, nobody needs to prove
        // they control the address before signing in.
        user.setEmailVerified(true);
        userRepository.save(user);
        return userMapper.toSummary(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummary> list(Integer page, Integer size) {
        var pageable = PageRequests.of(page, size);
        return userMapper.toSummaries(userRepository.findByRole(Role.SUB_ADMIN, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummary get(UUID id) {
        return userMapper.toSummary(findSubAdminOrThrow(id));
    }

    @Override
    @Transactional
    public UserSummary update(UUID id, SubAdminUpdateRequest request) {
        User user = findSubAdminOrThrow(id);
        if (!user.getEmail().equalsIgnoreCase(request.email())
                && userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered");
        }
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setMobile(request.mobile());
        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
            // A password change should end existing sessions, same as the customer-facing
            // password reset flow.
            refreshTokenRepository.revokeAllForUser(user.getId(), Instant.now());
        }
        return userMapper.toSummary(user);
    }

    @Override
    @Transactional
    public UserSummary deactivate(UUID id) {
        User user = findSubAdminOrThrow(id);
        user.setStatus(UserStatus.DISABLED);
        refreshTokenRepository.revokeAllForUser(user.getId(), Instant.now());
        return userMapper.toSummary(user);
    }

    @Override
    @Transactional
    public UserSummary activate(UUID id) {
        User user = findSubAdminOrThrow(id);
        user.setStatus(UserStatus.ACTIVE);
        return userMapper.toSummary(user);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        User user = findSubAdminOrThrow(id);
        // Safe hard delete: refresh_tokens/device_tokens/notification_outbox all cascade on
        // delete, and a Sub Admin never has orders, a wallet or a profile row.
        userRepository.delete(user);
    }

    /** Scoped so this service can never read or mutate a non-Sub-Admin account, even if a
     *  caller supplies another role's id. */
    private User findSubAdminOrThrow(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sub admin not found: " + id));
        if (user.getRole() != Role.SUB_ADMIN) {
            throw new ResourceNotFoundException("Sub admin not found: " + id);
        }
        return user;
    }
}
