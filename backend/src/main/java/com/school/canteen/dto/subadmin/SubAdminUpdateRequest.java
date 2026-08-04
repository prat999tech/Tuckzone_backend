package com.school.canteen.dto.subadmin;

import com.school.canteen.dto.auth.ValidationRules;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Super-Admin-only request to edit a Sub Admin account. {@code newPassword} is optional —
 *  omit it (null) to leave the password unchanged. */
public record SubAdminUpdateRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 180) String email,

        @NotBlank
        @Pattern(regexp = ValidationRules.MOBILE_PATTERN, message = ValidationRules.MOBILE_MESSAGE)
        String mobile,

        @Size(min = ValidationRules.PASSWORD_MIN, max = ValidationRules.PASSWORD_MAX,
                message = ValidationRules.PASSWORD_MESSAGE)
        String newPassword) {
}
