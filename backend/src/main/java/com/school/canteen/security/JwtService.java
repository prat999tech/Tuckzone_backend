package com.school.canteen.security;

import com.school.canteen.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies JWTs. A token is a signed statement "this is user X, until time T".
 * We sign with an HMAC key derived from the configured secret, so only this server (which
 * holds the secret) can produce a token that verifies — no DB lookup needed to trust it.
 *
 * Two token types are distinguished by the "type" claim:
 *   - access  : short-lived, sent on every request.
 *   - refresh : long-lived, used only to mint new access tokens.
 */
@Service
public class JwtService {

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ROLE = "role";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey signingKey;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtService(JwtProperties properties) {
        // hmacShaKeyFor requires >= 256 bits (32 bytes); our configured secret is longer.
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes());
        this.accessTtl = Duration.ofMinutes(properties.accessTokenTtlMinutes());
        this.refreshTtl = Duration.ofDays(properties.refreshTokenTtlDays());
    }

    public String generateAccessToken(String email, String role) {
        return build(email, role, TYPE_ACCESS, accessTtl);
    }

    public String generateRefreshToken(String email, String role) {
        return build(email, role, TYPE_REFRESH, refreshTtl);
    }

    private String build(String email, String role, String type, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(signingKey)
                .compact();
    }

    /** Parses + verifies signature and expiry. Throws JwtException if invalid/expired. */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token) {
        return parse(token).getSubject();
    }

    public boolean isRefreshToken(Claims claims) {
        return TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class));
    }

    public boolean isAccessToken(Claims claims) {
        return TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class));
    }

    public long accessTtlSeconds() {
        return accessTtl.toSeconds();
    }
}
