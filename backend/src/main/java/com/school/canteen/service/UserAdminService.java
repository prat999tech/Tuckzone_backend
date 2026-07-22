package com.school.canteen.service;

import com.school.canteen.dto.UserSummary;
import com.school.canteen.enums.UserStatus;
import java.util.List;
import java.util.UUID;

/** School-administrator operations over user accounts (the approval gate). */
public interface UserAdminService {

    List<UserSummary> listByStatus(UserStatus status);

    UserSummary approve(UUID userId);

    UserSummary reject(UUID userId);
}
