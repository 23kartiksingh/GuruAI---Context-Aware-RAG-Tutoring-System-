package com.guruai.auth.dto.response;

import java.time.Instant;

/**
 * Response for {@code GET /auth/user/profile} and {@code PUT /auth/user/profile}.
 *
 * <p>Mapped from the {@link com.guruai.auth.entity.User} entity
 * by {@link com.guruai.auth.mapper.UserMapper}.
 *
 * @param userId    UUID as string
 * @param username  unique login name
 * @param name      display name
 * @param bio       user bio / learning goals
 * @param createdAt when the account was registered
 */
public record UserProfileResponse(
        String  userId,
        String  username,
        String  name,
        String  bio,
        Instant createdAt
) {}
