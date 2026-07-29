package com.guruai.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /auth/login}.
 *
 * @param username  the user's login name (case-insensitive — lowercased before lookup)
 * @param password  the user's plain-text password (BCrypt compared in service layer)
 */
public record LoginRequest(

        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {}
