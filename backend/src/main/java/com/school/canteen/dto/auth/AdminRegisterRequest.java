package com.school.canteen.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Canteen-admin registration.
 *
 * The signup code is what stops this from being a takeover route: without it, anyone who
 * can reach the public registration endpoint could create themselves an owner account
 * with full control of the menu, stock, orders and revenue.
 */
public record AdminRegisterRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 180) String email,

        @NotBlank
        @Pattern(regexp = ValidationRules.MOBILE_PATTERN, message = ValidationRules.MOBILE_MESSAGE)
        String mobile,

        @NotBlank
        @Size(min = ValidationRules.PASSWORD_MIN, max = ValidationRules.PASSWORD_MAX,
                message = ValidationRules.PASSWORD_MESSAGE)
        String password,

        @NotBlank String signupCode) {
}
