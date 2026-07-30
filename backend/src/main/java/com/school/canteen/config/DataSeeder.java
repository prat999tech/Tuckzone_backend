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
 * Creates a bootstrap canteen-admin account when {@code app.seed.enabled} is on.
 *
 * Off by default — admins register through /api/auth/register/admin with the invite code.
 * This remains available so a brand-new environment is not locked out before the first
 * admin has signed up. Idempotent: safe to run on every boot.
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
        SeedProperties.Account account = seedProperties.canteenAdmin();
        // Keyed on email only: mobile is contact information, not an identity, so two
        // accounts legitimately sharing a family phone number must not block seeding.
        if (account == null || userRepository.existsByEmail(account.email())) {
            return;
        }

        User user = new User();
        user.setFullName(account.fullName());
        user.setEmail(account.email());
        user.setMobile(account.mobile());
        user.setPasswordHash(passwordEncoder.encode(account.password()));
        user.setRole(Role.CANTEEN_ADMIN);
        user.setStatus(UserStatus.ACTIVE);
        // Password sign-in now requires a confirmed address. This account is created by
        // whoever configured the server rather than by someone filling in a form, so there
        // is no code to enter — without this it could never sign in, which would defeat the
        // whole point of a bootstrap escape hatch.
        user.setEmailVerified(true);
        userRepository.save(user);
        log.info("Seeded bootstrap CANTEEN_ADMIN account: {}", account.email());
    }
}
