package com.school.canteen.dto;

import com.school.canteen.enums.Role;
import com.school.canteen.enums.UserStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Safe, outward-facing view of a user. Notably it has no password field — entities never
 * leave the service layer directly, so a hash can't leak into a response by accident.
 */
public record UserSummary(
        UUID id,
        String fullName,
        String email,
        String mobile,
        Role role,
        UserStatus status,
        Instant createdAt,
        String admissionNumber,
        String studentClass,
        String section,
        String rollNumber,
        String employeeId,
        String department) {
}
