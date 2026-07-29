package com.guruai.auth.mapper;

import com.guruai.auth.dto.response.UserProfileResponse;
import com.guruai.auth.entity.User;
import org.springframework.stereotype.Component;

/**
 * Converts {@link User} entities to response DTOs.
 *
 * <p>Manual mapper (no MapStruct in Phase 2) — add MapStruct later if the
 * number of mappings grows. Annotated with {@code @Component} so it can be
 * injected into services via constructor injection.
 */
@Component
public class UserMapper {

    /**
     * Map a {@link User} entity to a {@link UserProfileResponse} DTO.
     *
     * @param user the JPA entity (never {@code null})
     * @return an immutable DTO safe to return from controllers
     */
    public UserProfileResponse toProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId().toString(),
                user.getUsername(),
                user.getName(),
                user.getBio(),
                user.getCreatedAt()
        );
    }
}
