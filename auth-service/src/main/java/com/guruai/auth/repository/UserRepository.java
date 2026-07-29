package com.guruai.auth.repository;

import com.guruai.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link User} entities.
 *
 * <p>All access to the {@code users} table must go through this interface.
 * Business logic belongs in the service layer — never here.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find a user by their unique login username.
     *
     * @param username case-sensitive username (lowercased before storage)
     * @return an {@link Optional} containing the user, or empty if not found
     */
    Optional<User> findByUsername(String username);

    /**
     * Check whether a username is already taken.
     *
     * @param username the username to check (case-sensitive)
     * @return {@code true} if the username exists
     */
    boolean existsByUsername(String username);

    /**
     * Find a user previously created/linked via an OAuth2 provider.
     *
     * @param authProvider e.g. {@code "GOOGLE"}
     * @param providerId   the provider's stable subject id (Google's {@code sub} claim)
     * @return an {@link Optional} containing the user, or empty on first-ever login
     */
    Optional<User> findByAuthProviderAndProviderId(String authProvider, String providerId);
}
