package com.guruai.auth.config;

import com.guruai.auth.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Stateless JWT authentication filter — runs once per request.
 *
 * <p>Token extraction order:
 * <ol>
 *   <li>Cookie named {@code access_token} (set by login/register endpoints)</li>
 *   <li>{@code Authorization: Bearer {token}} header (for API clients)</li>
 * </ol>
 *
 * <p>Validation steps:
 * <ol>
 *   <li>Parse and verify JWT signature</li>
 *   <li>Check JTI is not in Redis blacklist (set on logout)</li>
 *   <li>Populate {@link SecurityContextHolder} for downstream security checks</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String COOKIE_NAME        = "access_token";
    private static final String BLACKLIST_PREFIX   = "blacklist:jti:";

    private final JwtUtil             jwtUtil;
    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain         filterChain
    ) throws ServletException, IOException {

        String token = extractToken(request);

        if (StringUtils.hasText(token)) {
            Claims claims = jwtUtil.parseTokenSilently(token);

            if (claims != null && isNotBlacklisted(claims.getId())) {
                // Build authentication object — no roles in Phase 2 (all users equal)
                var auth = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(),            // principal = userId string
                        null,                           // credentials = null (stateless)
                        List.of()                       // authorities — empty for now
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("JWT authenticated userId={}", claims.getSubject());
            }
        }

        filterChain.doFilter(request, response);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Extract JWT from cookie first, then Authorization header.
     *
     * @param request the incoming HTTP request
     * @return the raw token string, or {@code null} if not found
     */
    private String extractToken(HttpServletRequest request) {
        // 1. Try httpOnly cookie (preferred — set by this service on login)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // 2. Fallback: Authorization: Bearer <token> (for Postman / API clients)
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        return null;
    }

    /**
     * Check that the token's JTI has not been added to the Redis blacklist.
     *
     * @param jti the JWT ID claim from the token
     * @return {@code true} if the token is NOT blacklisted (valid)
     */
    private boolean isNotBlacklisted(String jti) {
        Boolean exists = redisTemplate.hasKey(BLACKLIST_PREFIX + jti);
        return !Boolean.TRUE.equals(exists);
    }
}
