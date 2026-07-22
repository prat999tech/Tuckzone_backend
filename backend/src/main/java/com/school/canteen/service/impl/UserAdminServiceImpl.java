package com.school.canteen.service.impl;

import com.school.canteen.dto.UserSummary;
import com.school.canteen.entity.User;
import com.school.canteen.enums.UserStatus;
import com.school.canteen.exception.ResourceNotFoundException;
import com.school.canteen.mapper.UserMapper;
import com.school.canteen.repository.UserRepository;
import com.school.canteen.service.UserAdminService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAdminServiceImpl implements UserAdminService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserAdminServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummary> listByStatus(UserStatus status) {
        return userRepository.findByStatus(status).stream()
                .map(userMapper::toSummary)
                .toList();
    }

    @Override
    @Transactional
    public UserSummary approve(UUID userId) {
        return changeStatus(userId, UserStatus.APPROVED);
    }

    @Override
    @Transactional
    public UserSummary reject(UUID userId) {
        return changeStatus(userId, UserStatus.REJECTED);
    }

    private UserSummary changeStatus(UUID userId, UserStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setStatus(status);
        // No explicit save needed: the entity is managed within this transaction, so the
        // change is flushed on commit (JPA dirty checking). Kept explicit-free on purpose.
        return userMapper.toSummary(user);
    }
}
