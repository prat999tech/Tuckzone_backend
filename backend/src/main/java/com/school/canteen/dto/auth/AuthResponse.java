package com.school.canteen.dto.auth;

import com.school.canteen.dto.UserSummary;

/**
 * Returned on successful login/refresh. tokenType is always "Bearer"; expiresInSeconds is
 * the access token's lifetime so clients know when to refresh.
 */
public record AuthResponse(
        String tokenType,
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        UserSummary user) {
}
