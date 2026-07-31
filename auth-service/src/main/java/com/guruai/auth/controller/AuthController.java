package com.guruai.auth.controller;

import com.guruai.auth.dto.request.LoginRequest;
import com.guruai.auth.dto.request.RegisterRequest;
import com.guruai.auth.dto.response.AuthResponse;
import com.guruai.auth.service.AuthService;
import com.guruai.auth.service.TokenService;
import com.guruai.common.dto.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints.
 *
 * <p>Base path: {@code /auth}
 *
 * <table border="1">
 *   <tr><th>Method</th><th>Path</th><th>Auth</th><th>Description</th></tr>
 *   <tr><td>POST</td><td>/auth/register</td><td>PUBLIC</td><td>Create a new account</td></tr>
 *   <tr><td>POST</td><td>/auth/login</td><td>PUBLIC</td><td>Login with credentials</td></tr>
 *   <tr><td>POST</td><td>/auth/logout</td><td>JWT</td><td>Logout (blacklist + clear cookies)</td></tr>
 *   <tr><td>POST</td><td>/auth/refresh</td><td>PUBLIC (cookie)</td><td>Refresh access token</td></tr>
 * </table>
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService  authService;
    private final TokenService tokenService;

    /**
     * Register a new user account.
     *
     * <p>On success: sets {@code access_token} and {@code refresh_token} httpOnly cookies.
     * Returns the access token in the response body for API clients.
     *
     * @param request  validated registration fields
     * @param response servlet response — cookies attached here
     * @return 201 Created with {@link AuthResponse}
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {

        AuthResponse auth = authService.register(request, response);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(auth));
    }

    /**
     * Authenticate with username and password.
     *
     * <p>On success: sets {@code access_token} and {@code refresh_token} httpOnly cookies.
     * Returns the access token in the response body for API clients.
     *
     * @param request  login credentials
     * @param response servlet response — cookies attached here
     * @return 200 OK with {@link AuthResponse}
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthResponse auth = authService.login(request, response);
        return ResponseEntity.ok(ApiResponse.ok(auth, "Login successful"));
    }

    /**
     * Logout the current user.
     *
     * <p>Blacklists the access token JTI in Redis, revokes the refresh token in DB,
     * and clears both httpOnly cookies.
     *
     * @param request  servlet request — reads cookies
     * @param response servlet response — clears cookies
     * @return 200 OK
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest  request,
            HttpServletResponse response) {

        String accessToken  = extractCookie(request, "access_token");
        String refreshToken = extractCookie(request, "refresh_token");

        authService.logout(accessToken, refreshToken, response);
        return ResponseEntity.ok(ApiResponse.ok(null, "Logged out successfully"));
    }

    /**
     * Refresh the access token using the refresh_token cookie.
     *
     * <p>Validates the refresh token, issues a new access token (and rotates the
     * refresh token), and updates both cookies.
     *
     * @param request  servlet request — reads refresh_token cookie
     * @param response servlet response — updates cookies
     * @return 200 OK with new {@link AuthResponse}
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refresh(
            HttpServletRequest  request,
            HttpServletResponse response) {

        String refreshToken = extractCookie(request, "refresh_token");
        String newAccessToken = tokenService.refreshAccessToken(refreshToken, response);
        return ResponseEntity.ok(ApiResponse.ok(newAccessToken, "Token refreshed"));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }
}
