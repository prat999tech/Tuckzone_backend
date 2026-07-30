package com.school.canteen.service;

import com.school.canteen.dto.UserSummary;
import com.school.canteen.enums.Role;
import java.util.List;
import java.util.UUID;

/**
 * Account administration for the canteen operator.
 *
 * The registration-approval workflow is gone: the canteen is vendor-run and nobody at the
 * school vets sign-ups. What remains is the ability to block an account that is being
 * abused, which takes effect immediately because the JWT filter re-checks status on every
 * request and every session is revoked on disable.
 */
public interface UserAdminService {

    List<UserSummary> listUsers(Role role, Integer page, Integer size);

    UserSummary disable(UUID userId);

    UserSummary enable(UUID userId);
}
