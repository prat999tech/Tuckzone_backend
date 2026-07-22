package com.school.canteen.config;

import com.school.canteen.entity.User;
import com.school.canteen.enums.Role;
import com.school.canteen.enums.UserStatus;
import com.school.canteen.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the bootstrap admin accounts on startup if they don't already exist. Admins
 * can't self-register (no public admin role), so the system needs at least one to exist
 * for the approval gate to be operable. Idempotent: safe to run on every boot.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final SeedProperties seedProperties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(SeedProperties seedProperties, UserRepository userRepository,
                      PasswordEncoder passwordEncoder) {
        this.seedProperties = seedProperties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedProperties.enabled()) {
            return;
        }
        seedAdmin(seedProperties.schoolAdmin(), Role.SCHOOL_ADMIN);
        seedAdmin(seedProperties.canteenAdmin(), Role.CANTEEN_ADMIN);
    }

    private void seedAdmin(SeedProperties.Account account, Role role) {
        if (account == null || userRepository.existsByEmail(account.email())) {
            return;
        }
        User user = new User();
        user.setFullName(account.fullName());
        user.setEmail(account.email());
        user.setMobile(account.mobile());
        user.setPasswordHash(passwordEncoder.encode(account.password()));
        user.setRole(role);
        user.setStatus(UserStatus.APPROVED); // admins are active immediately
        userRepository.save(user);
        log.info("Seeded {} account: {}", role, account.email());
    }
}
