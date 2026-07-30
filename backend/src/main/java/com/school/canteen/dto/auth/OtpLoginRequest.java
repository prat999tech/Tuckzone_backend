package com.school.canteen.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Signs in with an email address and the passcode sent to it. */
public record OtpLoginRequest(
        @NotBlank @Email @Size(max = 180) String email,
        @NotBlank String code) {
}
