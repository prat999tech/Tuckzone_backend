package com.school.canteen.service.impl;

import com.school.canteen.dto.UserSummary;
import com.school.canteen.entity.User;
import com.school.canteen.enums.Role;
import com.school.canteen.enums.UserStatus;
import com.school.canteen.exception.BadRequestException;
import com.school.canteen.exception.ResourceNotFoundException;
import com.school.canteen.mapper.UserMapper;
import com.school.canteen.repository.RefreshTokenRepository;
import com.school.canteen.repository.UserRepository;
import com.school.canteen.service.UserAdminService;
import com.school.canteen.util.PageRequests;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAdminServiceImpl implements UserAdminService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;

    public UserAdminServiceImpl(UserRepository userRepository,
                                RefreshTokenRepository refreshTokenRepository,
                                UserMapper userMapper) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummary> listUsers(Role role, Integer page, Integer size) {
        var pageable = PageRequests.of(page, size);
        List<User> users = (role == null)
                ? userRepository.findByStatus(UserStatus.ACTIVE, pageable)
                : userRepository.findByRole(role, pageable);
        // Batch mapping: two profile queries total instead of two per user.
        return userMapper.toSummaries(users);
    }

    @Override
    @Transactional
    public UserSummary disable(UUID userId) {
        User user = findOrThrow(userId);
        if (user.getRole() == Role.CANTEEN_ADMIN) {
            // Prevents the operator locking themselves (and everyone else) out of the
            // only role that can re-enable accounts.
            throw new BadRequestException("A canteen admin account cannot be disabled here");
        }
        user.setStatus(UserStatus.DISABLED);
        // Kill live sessions too: without this the user keeps working until their access
        // token expires and could still spend their wallet balance.
        refreshTokenRepository.revokeAllForUser(userId, Instant.now());
        return userMapper.toSummary(user);
    }

    @Override
    @Transactional
    public UserSummary enable(UUID userId) {
        User user = findOrThrow(userId);
        user.setStatus(UserStatus.ACTIVE);
        return userMapper.toSummary(user);
    }

    private User findOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
