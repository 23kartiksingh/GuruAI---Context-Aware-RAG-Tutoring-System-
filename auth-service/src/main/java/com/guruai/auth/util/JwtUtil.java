package com.guruai.auth.util;

import com.guruai.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Utility for generating and parsing JWT tokens using JJWT 0.12.x.
 *
 * <p>Two types of tokens are issued:
 * <ul>
 *   <li><b>Access Token</b>  — HS512 signed, 1-hour TTL, carries userId + username + jti</li>
 *   <li><b>Refresh Token</b> — HS512 signed, 14-day TTL, carries userId only</li>
 * </ul>
 *
 * <p>The access token JTI (jti claim) is used as the Redis blacklist key on logout.
 */
@Slf4j
@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final long      accessExpiryMs;
    private final long      refreshExpiryMs;

    public JwtUtil(JwtProperties props) {
        // Use UTF-8 bytes of the raw secret — must be ≥ 64 chars (enforced in JwtProperties)
        this.signingKey      = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
        this.accessExpiryMs  = props.accessExpiryMs();
        this.refreshExpiryMs = props.refreshExpiryMs();
    }

    // ── Token Generation ──────────────────────────────────────────────────────

    /**
     * Generate a new JWT access token.
     *
     * @param userId   UUID string of the authenticated user
     * @param username the user's login name
     * @return compact signed JWT string
     */
    public String generateAccessToken(String userId, String username) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + accessExpiryMs);

        return Jwts.builder()
                .subject(userId)
                .claim("username", username)
                .claim("type", "access")
                .id(UUID.randomUUID().toString())   // JTI — used for blacklisting
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generate a new JWT refresh token.
     *
     * @param userId UUID string of the authenticated user
     * @return compact signed JWT string
     */
    public String generateRefreshToken(String userId) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + refreshExpiryMs);

        return Jwts.builder()
                .subject(userId)
                .claim("type", "refresh")
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    // ── Token Parsing ─────────────────────────────────────────────────────────

    /**
     * Parse and validate a JWT token.
     *
     * @param token the compact JWT string
     * @return {@link Claims} payload if valid
     * @throws JwtException if the token is malformed, expired, or has invalid signature
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Attempt to parse a token, returning null on any failure (used in filter).
     * Logs a debug message instead of throwing — caller decides how to respond.
     *
     * @param token the compact JWT string
     * @return {@link Claims} or {@code null} if parsing fails
     */
    public Claims parseTokenSilently(String token) {
        try {
            return parseToken(token);
        } catch (ExpiredJwtException e) {
            log.debug("JWT token expired: {}", e.getMessage());
        } catch (JwtException e) {
            log.debug("JWT token invalid: {}", e.getMessage());
        }
        return null;
    }

    // ── Claim Extractors ──────────────────────────────────────────────────────

    /** Extract the subject (userId) from a token without full validation. */
    public String extractUserId(String token) {
        return parseToken(token).getSubject();
    }

    /** Extract the JTI (unique token ID) used for blacklisting. */
    public String extractJti(String token) {
        return parseToken(token).getId();
    }

    /** Extract the username claim from an access token. */
    public String extractUsername(String token) {
        return parseToken(token).get("username", String.class);
    }

    /**
     * Calculate the remaining TTL of a token.
     *
     * @param claims parsed claims
     * @return remaining duration in milliseconds (0 if already expired)
     */
    public long getRemainingTtlMs(Claims claims) {
        long expiryMs = claims.getExpiration().toInstant().toEpochMilli();
        long nowMs    = Instant.now().toEpochMilli();
        return Math.max(0, expiryMs - nowMs);
    }

    /** @return access token TTL in milliseconds (for cookie max-age) */
    public long getAccessExpiryMs() {
        return accessExpiryMs;
    }

    /** @return refresh token TTL in milliseconds (for cookie max-age) */
    public long getRefreshExpiryMs() {
        return refreshExpiryMs;
    }

    /**
     * Calculate the Instant at which a new refresh token expires.
     * Called when storing the token in the database.
     */
    public Instant refreshTokenExpiresAt() {
        return Instant.now().plusMillis(refreshExpiryMs);
    }
}
