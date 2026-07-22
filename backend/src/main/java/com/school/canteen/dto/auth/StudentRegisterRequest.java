package com.school.canteen.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Self-registration payload for a student. Validation here is the first gate; uniqueness
 * (email, admission number) is enforced authoritatively by the database.
 */
public record StudentRegisterRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 180) String email,
        @NotBlank @Size(max = 20) String mobile,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 40) String admissionNumber,
        @NotBlank @Size(max = 20) String studentClass,
        @NotBlank @Size(max = 10) String section,
        @NotBlank @Size(max = 20) String rollNumber,
        @NotBlank @Size(max = 20) String parentMobile,
        @Size(max = 20) String studentMobile) {
}
