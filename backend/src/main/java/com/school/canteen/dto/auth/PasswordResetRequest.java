package com.school.canteen.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Sets a new password after proving control of the email address with a passcode.
 * Serves both "forgot my password" and a deliberate change by a signed-in user.
 */
public record PasswordResetRequest(
        @NotBlank @Email @Size(max = 180) String email,

        @NotBlank String code,

        @NotBlank
        @Size(min = ValidationRules.PASSWORD_MIN, max = ValidationRules.PASSWORD_MAX,
                message = ValidationRules.PASSWORD_MESSAGE)
        String newPassword) {
}
