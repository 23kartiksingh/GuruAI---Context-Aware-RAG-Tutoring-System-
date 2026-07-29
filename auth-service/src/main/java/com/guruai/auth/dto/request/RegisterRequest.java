package com.guruai.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /auth/register}.
 *
 * <p>All fields are validated by Bean Validation before reaching the service layer.
 *
 * @param username  desired login name — 3–100 chars, will be lowercased
 * @param password  plain-text password — 6–100 chars — BCrypt-hashed before storage
 * @param name      optional display name — defaults to "The Scholar" if blank
 */
public record RegisterRequest(

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 100, message = "Username must be 3–100 characters")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 100, message = "Password must be 6–100 characters")
        String password,

        @Size(max = 200, message = "Name must not exceed 200 characters")
        String name
) {}
