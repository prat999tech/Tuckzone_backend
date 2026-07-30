package com.school.canteen.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TeacherRegisterRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 180) String email,

        @NotBlank
        @Pattern(regexp = ValidationRules.MOBILE_PATTERN, message = ValidationRules.MOBILE_MESSAGE)
        String mobile,

        @NotBlank
        @Size(min = ValidationRules.PASSWORD_MIN, max = ValidationRules.PASSWORD_MAX,
                message = ValidationRules.PASSWORD_MESSAGE)
        String password,

        /** Staff / employee number issued by the school. */
        @NotBlank @Size(max = 40) String employeeId,

        @NotBlank @Size(max = 80) String department) {
}
