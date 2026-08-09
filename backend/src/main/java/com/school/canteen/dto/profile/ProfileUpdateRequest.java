package com.school.canteen.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Fields a user may change about themselves.
 *
 * Deliberately excluded:
 * <ul>
 *   <li>admission number — parent-child links are anchored to it, so editing it would
 *       silently break an existing link;</li>
 *   <li>parent mobile — it is the secret half of the link check; letting a student rewrite
 *       it would let them hand their account to any adult;</li>
 *   <li>employee id — the school issues it;</li>
 *   <li>mobile and email — they are login identities and changing them needs its own
 *       passcode-verified flow rather than a silent profile edit.</li>
 * </ul>
 */
public record ProfileUpdateRequest(
        @NotBlank @Size(max = 120) String fullName,

        // Student-only fields; ignored for other roles.
        @Size(max = 20) String studentClass,
        @Size(max = 10) String section,
        @Size(max = 20) String rollNumber,
        @Size(max = 20) String seatNumber,

        // Teacher-only field; ignored for other roles.
        @Size(max = 80) String department) {
}
