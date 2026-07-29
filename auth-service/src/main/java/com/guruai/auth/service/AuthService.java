package com.guruai.auth.service;

import com.guruai.auth.dto.request.LoginRequest;
import com.guruai.auth.dto.request.RegisterRequest;
import com.guruai.auth.dto.request.UpdateProfileRequest;
import com.guruai.auth.dto.response.AuthResponse;
import com.guruai.auth.dto.response.UserProfileResponse;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Contract for all authentication and user-management operations.
 *
 * <p>Implemented by {@link com.guruai.auth.service.impl.AuthServiceImpl}.
 */
public interface AuthService {

    /**
     * Register a new user account.
     *
     * <p>Side effects:
     * <ul>
     *   <li>Creates a new {@link com.guruai.auth.entity.User} row in auth_db</li>
     *   <li>Issues access + refresh tokens and sets httpOnly cookies</li>
     *   <li>Publishes {@code user.registered} Kafka event</li>
     * </ul>
     *
     * @param request  validated registration fields
     * @param response HTTP response — used to attach httpOnly cookies
     * @return {@link AuthResponse} with access token and user identity
     * @throws com.guruai.common.exception.GuruAIException (409) if username taken
     */
    AuthResponse register(RegisterRequest request, HttpServletResponse response);

    /**
     * Authenticate an existing user.
     *
     * <p>Side effects:
     * <ul>
     *   <li>Verifies BCrypt password</li>
     *   <li>Issues access + refresh tokens and sets httpOnly cookies</li>
     * </ul>
     *
     * @param request  login credentials
     * @param response HTTP response — used to attach httpOnly cookies
     * @return {@link AuthResponse} with access token and user identity
     * @throws com.guruai.common.exception.GuruAIException (401) if credentials invalid
     */
    AuthResponse login(LoginRequest request, HttpServletResponse response);

    /**
     * Find-or-create a user from a verified Google profile and log them in.
     *
     * <p>Matched on {@code (authProvider, providerId)} — Google's stable
     * {@code sub} claim — never on email, so this never merges into an
     * existing LOCAL (username/password) account. First-ever call for a
     * given Google account creates one (also publishes {@code user.registered}
     * and derives a unique username from the email's local part); every call
     * after that is just a login.
     *
     * <p>Side effects: same as {@link #login} — issues tokens and sets
     * httpOnly cookies.
     *
     * @param googleId Google's stable subject identifier (the {@code sub} claim)
     * @param email    verified email from Google's userinfo response (may be null)
     * @param name     display name from Google's profile (may be null/blank)
     * @param response HTTP response — used to attach httpOnly cookies
     * @return {@link AuthResponse} with access token and user identity
     */
    AuthResponse loginWithGoogle(String googleId, String email, String name, HttpServletResponse response);

    /**
     * Log out the current user.
     *
     * <p>Side effects:
     * <ul>
     *   <li>Blacklists the access token JTI in Redis (TTL = remaining token lifetime)</li>
     *   <li>Revokes the refresh token in the database</li>
     *   <li>Clears the access_token and refresh_token cookies</li>
     * </ul>
     *
     * @param accessToken  the raw JWT access token string
     * @param refreshToken the raw JWT refresh token string (from cookie)
     * @param response     HTTP response — used to clear cookies
     */
    void logout(String accessToken, String refreshToken, HttpServletResponse response);

    /**
     * Get the profile of the currently authenticated user.
     *
     * @param userId UUID string from JWT principal
     * @return {@link UserProfileResponse} DTO
     * @throws com.guruai.common.exception.ResourceNotFoundException if user not found
     */
    UserProfileResponse getProfile(String userId);

    /**
     * Update the display name and/or bio of the authenticated user.
     * Only non-null fields in the request are updated.
     *
     * @param userId  UUID string from JWT principal
     * @param request fields to update (both optional)
     * @return updated {@link UserProfileResponse}
     */
    UserProfileResponse updateProfile(String userId, UpdateProfileRequest request);
}
