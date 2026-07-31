package com.guruai.auth.controller;

import com.guruai.auth.dto.request.UpdateProfileRequest;
import com.guruai.auth.dto.response.UserProfileResponse;
import com.guruai.auth.service.AuthService;
import com.guruai.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user profile endpoints.
 *
 * <p>Base path: {@code /auth/user}
 *
 * <p>All endpoints require a valid JWT (enforced by {@link com.guruai.auth.config.SecurityConfig}).
 *
 * <table border="1">
 *   <tr><th>Method</th><th>Path</th><th>Description</th></tr>
 *   <tr><td>GET</td><td>/auth/user/profile</td><td>Get profile of logged-in user</td></tr>
 *   <tr><td>PUT</td><td>/auth/user/profile</td><td>Update name and/or bio</td></tr>
 * </table>
 *
 * <p>{@code @AuthenticationPrincipal} receives the JWT subject (userId string)
 * set by {@link com.guruai.auth.config.JwtAuthFilter}.
 */
@RestController
@RequestMapping("/auth/user")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    /**
     * Get the profile of the currently authenticated user.
     *
     * @param userId injected from JWT principal (set by JwtAuthFilter)
     * @return 200 OK with {@link UserProfileResponse}
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal String userId) {

        UserProfileResponse profile = authService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    /**
     * Update the authenticated user's display name and/or bio.
     *
     * @param userId  injected from JWT principal
     * @param request fields to update (both optional — partial update)
     * @return 200 OK with updated {@link UserProfileResponse}
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody UpdateProfileRequest request) {

        UserProfileResponse updated = authService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Profile updated successfully"));
    }
}
