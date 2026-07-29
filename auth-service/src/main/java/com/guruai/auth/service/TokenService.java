package com.guruai.auth.service;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Contract for JWT token lifecycle operations (issue, refresh, revoke).
 *
 * <p>Implemented by {@link com.guruai.auth.service.impl.TokenServiceImpl}.
 * Deliberately separate from {@link AuthService} — token operations are
 * an independent concern that may be extracted later.
 */
public interface TokenService {

    /**
     * Set httpOnly access and refresh token cookies on the HTTP response.
     *
     * @param userId       the user UUID to embed in the tokens
     * @param username     the username claim for the access token
     * @param httpResponse the servlet response to add cookies to
     * @return the raw access token string (returned in body for API clients)
     */
    String issueTokensAndSetCookies(String userId, String username,
                                    HttpServletResponse httpResponse);

    /**
     * Refresh the access token using a valid refresh token.
     *
     * <p>Validates that the refresh token:
     * <ul>
     *   <li>Exists in the database</li>
     *   <li>Is not revoked</li>
     *   <li>Has not expired</li>
     * </ul>
     *
     * @param refreshToken raw JWT refresh token from the cookie
     * @param httpResponse the servlet response to update cookies
     * @return new raw access token string
     * @throws com.guruai.common.exception.GuruAIException (401) if refresh token is invalid
     */
    String refreshAccessToken(String refreshToken, HttpServletResponse httpResponse);

    /**
     * Blacklist the access token JTI in Redis and revoke the refresh token in DB.
     *
     * @param accessToken  raw JWT access token to blacklist
     * @param refreshToken raw JWT refresh token to revoke in DB
     */
    void revokeTokens(String accessToken, String refreshToken);

    /**
     * Clear the access_token and refresh_token httpOnly cookies from the response.
     *
     * @param httpResponse the servlet response to clear cookies on
     */
    void clearCookies(HttpServletResponse httpResponse);
}
