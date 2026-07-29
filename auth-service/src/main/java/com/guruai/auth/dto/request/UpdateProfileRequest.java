package com.guruai.auth.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Request body for {@code PUT /auth/user/profile}.
 *
 * <p>All fields are optional — only non-null fields will be updated.
 * This supports partial PATCH-style semantics via PUT.
 *
 * @param name  new display name (1–200 chars if provided)
 * @param bio   new bio / learning goals (max 2 000 chars)
 */
public record UpdateProfileRequest(

        @Size(min = 1, max = 200, message = "Name must be 1–200 characters")
        String name,

        @Size(max = 2000, message = "Bio must not exceed 2 000 characters")
        String bio
) {}
