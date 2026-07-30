package com.school.canteen.dto.auth;

import com.school.canteen.enums.OtpPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Asks for a passcode to be emailed to a registered address. */
public record OtpRequest(
        @NotBlank @Email @Size(max = 180) String email,
        @NotNull OtpPurpose purpose) {
}
