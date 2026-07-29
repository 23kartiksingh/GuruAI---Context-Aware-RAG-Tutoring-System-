package com.guruai.auth.dto.response;

/**
 * Response for {@code POST /auth/register} and {@code POST /auth/login}.
 *
 * <p>The access token is returned both in this body AND as an httpOnly cookie
 * named {@code access_token}. The refresh token is ONLY in the cookie (never in body).
 *
 * @param accessToken JWT access token (1 hour TTL)
 * @param userId      UUID of the authenticated user (for client-side state)
 * @param username    login name
 * @param name        display name
 * @param tokenType   always "Bearer"
 * @param expiresIn   access token TTL in seconds (matches the cookie max-age)
 */
public record AuthResponse(
        String accessToken,
        String userId,
        String username,
        String name,
        String tokenType,
        long   expiresIn
) {
    /** Convenience factory — tokenType defaults to "Bearer". */
    public static AuthResponse of(String accessToken, String userId,
                                   String username, String name, long expiresInMs) {
        return new AuthResponse(
                accessToken,
                userId,
                username,
                name,
                "Bearer",
                expiresInMs / 1000   // convert ms → seconds
        );
    }
}
