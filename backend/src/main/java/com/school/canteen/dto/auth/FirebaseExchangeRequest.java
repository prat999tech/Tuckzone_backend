package com.school.canteen.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Presented after the frontend completes sign-in with Firebase (phone OTP or email) —
 * never contains a uid, email, or role directly, only the token the backend re-verifies
 * itself. See {@link com.school.canteen.auth.firebase.FirebaseAuthService}.
 */
public record FirebaseExchangeRequest(@NotBlank String idToken) {
}
