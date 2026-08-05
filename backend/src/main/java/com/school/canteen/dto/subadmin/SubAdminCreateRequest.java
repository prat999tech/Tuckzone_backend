package com.school.canteen.dto.subadmin;

import com.school.canteen.dto.auth.ValidationRules;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Super-Admin-only request to create a new Sub Admin account. Role is never part of this
 *  request — the service always assigns Role.SUB_ADMIN, so this endpoint can never be used
 *  to create any other kind of account. */
public record SubAdminCreateRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 180) String email,

        @NotBlank
        @Pattern(regexp = ValidationRules.MOBILE_PATTERN, message = ValidationRules.MOBILE_MESSAGE)
        String mobile,

        @NotBlank
        @Size(min = ValidationRules.PASSWORD_MIN, max = ValidationRules.PASSWORD_MAX,
                message = ValidationRules.PASSWORD_MESSAGE)
        String password) {
}
