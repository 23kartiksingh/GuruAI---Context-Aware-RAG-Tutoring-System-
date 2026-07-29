package com.guruai.auth.service.impl;

import com.guruai.auth.config.CookieProperties;
import com.guruai.auth.entity.RefreshToken;
import com.guruai.auth.repository.RefreshTokenRepository;
import com.guruai.auth.service.TokenService;
import com.guruai.auth.util.JwtUtil;
import com.guruai.common.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

/**
 * Implementation of {@link TokenService}.
 *
 * <p>Manages the full token lifecycle:
 * <ol>
 *   <li>Issue (generate + set cookies + persist refresh to DB)</li>
 *   <li>Refresh (validate + new access token + rotate refresh token)</li>
 *   <li>Revoke (Redis blacklist + DB revoke)</li>
 *   <li>Clear cookies</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private static final String COOKIE_ACCESS   = "access_token";
    private static final String COOKIE_REFRESH  = "refresh_token";
    private static final String BLACKLIST_PREFIX = "blacklist:jti:";

    private final JwtUtil                jwtUtil;
    private final RefreshTokenRepository refreshTokenRepo;
    private final StringRedisTemplate    redisTemplate;
    private final CookieProperties       cookieProperties;

    // ── Issue ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public String issueTokensAndSetCookies(String userId, String username,
                                           HttpServletResponse response) {
        // Generate tokens
        String accessToken  = jwtUtil.generateAccessToken(userId, username);
        String refreshToken = jwtUtil.generateRefreshToken(userId);

        // Persist refresh token to DB
        RefreshToken rtEntity = RefreshToken.of(
                UUID.fromString(userId), refreshToken, jwtUtil.refreshTokenExpiresAt());
        refreshTokenRepo.save(rtEntity);

        // Set httpOnly cookies
        addCookie(response, COOKIE_ACCESS,  accessToken,  jwtUtil.getAccessExpiryMs());
        addCookie(response, COOKIE_REFRESH, refreshToken, jwtUtil.getRefreshExpiryMs());

        log.debug("Issued tokens for userId={}", userId);
        return accessToken;
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public String refreshAccessToken(String refreshToken, HttpServletResponse response) {
        // 1. Look up the refresh token in DB
        RefreshToken stored = refreshTokenRepo.findByToken(refreshToken)
                .orElseThrow(UnauthorizedException::invalidToken);

        // 2. Check validity (not revoked, not expired)
        if (stored.isInvalid()) {
            log.warn("Invalid refresh token for userId={}", stored.getUserId());
            throw UnauthorizedException.invalidToken();
        }

        // 3. Revoke the old refresh token (rotation — one-time use)
        stored.setRevoked(true);
        refreshTokenRepo.save(stored);

        // 4. Issue a new access token (and a new refresh token — sliding window)
        String userId   = stored.getUserId().toString();
        String newAccess  = jwtUtil.generateAccessToken(userId, "");  // username loaded by filter
        String newRefresh = jwtUtil.generateRefreshToken(userId);

        // 5. Persist new refresh token
        RefreshToken newRt = RefreshToken.of(
                stored.getUserId(), newRefresh, jwtUtil.refreshTokenExpiresAt());
        refreshTokenRepo.save(newRt);

        // 6. Update cookies
        addCookie(response, COOKIE_ACCESS,  newAccess,  jwtUtil.getAccessExpiryMs());
        addCookie(response, COOKIE_REFRESH, newRefresh, jwtUtil.getRefreshExpiryMs());

        log.debug("Refreshed access token for userId={}", userId);
        return newAccess;
    }

    // ── Revoke ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void revokeTokens(String accessToken, String refreshToken) {
        // 1. Blacklist access token JTI in Redis (TTL = remaining lifetime)
        if (accessToken != null) {
            Claims claims = jwtUtil.parseTokenSilently(accessToken);
            if (claims != null) {
                long ttlMs = jwtUtil.getRemainingTtlMs(claims);
                if (ttlMs > 0) {
                    redisTemplate.opsForValue().set(
                            BLACKLIST_PREFIX + claims.getId(),
                            "1",
                            Duration.ofMillis(ttlMs)
                    );
                    log.debug("Blacklisted JTI={} for {}ms", claims.getId(), ttlMs);
                }
            }
        }

        // 2. Revoke refresh token in DB
        if (refreshToken != null) {
            refreshTokenRepo.findByToken(refreshToken).ifPresent(rt -> {
                rt.setRevoked(true);
                refreshTokenRepo.save(rt);
                log.debug("Revoked refresh token for userId={}", rt.getUserId());
            });
        }
    }

    // ── Clear Cookies ─────────────────────────────────────────────────────────

    @Override
    public void clearCookies(HttpServletResponse response) {
        clearCookie(response, COOKIE_ACCESS);
        clearCookie(response, COOKIE_REFRESH);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void addCookie(HttpServletResponse response, String name,
                           String value, long maxAgeMs) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .path("/")
                .maxAge(Duration.ofMillis(maxAgeMs))
                .sameSite(cookieProperties.sameSite())
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .path("/")
                .maxAge(Duration.ZERO)    // maxAge=0 tells browser to delete the cookie
                .sameSite(cookieProperties.sameSite())
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
