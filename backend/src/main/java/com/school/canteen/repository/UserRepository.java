package com.school.canteen.repository;

import com.school.canteen.entity.User;
import com.school.canteen.enums.Role;
import com.school.canteen.enums.UserStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data derives the SQL for these methods from their names at startup — we declare
 * intent, the framework implements it. Extending JpaRepository gives us save/findById/etc.
 * for free.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByStatus(UserStatus status, Pageable pageable);

    List<User> findByRole(Role role, Pageable pageable);

    /** Used to broadcast canteen-wide announcements to active customers only. */
    List<User> findByRoleAndStatus(Role role, UserStatus status, Pageable pageable);
}
