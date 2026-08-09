package com.school.canteen.security;

import com.school.canteen.dto.auth.AuthResponse;
import com.school.canteen.entity.RefreshToken;
import com.school.canteen.entity.User;
import com.school.canteen.mapper.UserMapper;
import com.school.canteen.repository.RefreshTokenRepository;
import com.school.canteen.util.TokenHasher;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Mints the access/refresh token pair for an already-authenticated {@link User} and records
 * the refresh session, so it can be revoked later (logout, password change).
 *
 * Extracted out of {@code AuthServiceImpl} so every sign-in path — password, email OTP, and
 * Firebase — issues identical sessions through one piece of code. All of them are then
 * authorized the same way afterward: {@code JwtAuthenticationFilter} only ever sees this
 * server's own JWT, never a Firebase token, so nothing about API authorization, refresh,
 * logout, or rate limiting changes based on which provider verified the sign-in.
 */
@Component
public class SessionIssuer {

    private static final String TOKEN_TYPE = "Bearer";

    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;

    public SessionIssuer(JwtService jwtService, RefreshTokenRepository refreshTokenRepository,
                         UserMapper userMapper) {
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userMapper = userMapper;
    }

    public AuthResponse issue(User user) {
        String role = user.getRole().name();
        String accessToken = jwtService.generateAccessToken(user.getEmail(), role);
        String refreshToken = jwtService.generateRefreshToken(user.getEmail(), role);

        RefreshToken stored = new RefreshToken();
        stored.setUser(user);
        stored.setTokenHash(TokenHasher.sha256Hex(refreshToken));
        stored.setExpiresAt(Instant.now().plus(jwtService.refreshTtl()));
        refreshTokenRepository.save(stored);

        return new AuthResponse(
                TOKEN_TYPE,
                accessToken,
                refreshToken,
                jwtService.accessTtlSeconds(),
                userMapper.toSummary(user));
    }
}
