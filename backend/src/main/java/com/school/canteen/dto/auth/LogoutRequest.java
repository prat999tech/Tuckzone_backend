package com.school.canteen.dto.auth;

import jakarta.validation.constraints.NotBlank;

/** Ends the session belonging to this refresh token (this device only). */
public record LogoutRequest(
        @NotBlank String refreshToken) {
}
