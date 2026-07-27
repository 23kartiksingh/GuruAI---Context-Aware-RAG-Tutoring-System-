package com.guruai.auth.repository;

import com.guruai.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link RefreshToken} entities.
 *
 * <p>All access to the {@code refresh_tokens} table must go through this interface.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find a refresh token by its raw JWT string.
     * Used during the token refresh and logout flows.
     *
     * @param token the raw JWT refresh token string
     * @return {@link Optional} with the entity, or empty if not found
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Hard-delete all refresh tokens belonging to a user.
     * Called on "logout all devices".
     *
     * @param userId the user whose tokens to delete
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.userId = :userId")
    void deleteAllByUserId(UUID userId);

    /**
     * Nightly cleanup — remove all expired tokens to keep the table lean.
     *
     * @param now current timestamp
     * @return number of rows deleted
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    int deleteAllExpiredBefore(Instant now);
}
