package com.guruai.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapped to the {@code refresh_tokens} table in {@code auth_db}.
 *
 * <p>Schema managed by Flyway — see {@code V2__create_refresh_tokens_table.sql}.
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>Created on login or register</li>
 *   <li>Verified on /auth/refresh — if expired or revoked, reject</li>
 *   <li>Revoked (revoked=true) on /auth/logout</li>
 *   <li>Hard-deleted by a cleanup scheduled task (nightly, removes expired rows)</li>
 * </ol>
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The user who owns this token. Cascade-deleted when user is deleted. */
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /** The raw JWT refresh token string. Unique per row. */
    @Column(name = "token", nullable = false, unique = true, columnDefinition = "TEXT")
    private String token;

    /** When this refresh token expires (now + 14 days at creation). */
    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    /** Set to {@code true} on explicit logout. Prevents reuse even before expiry. */
    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        revoked   = false;
    }

    /** Factory method for cleaner construction. */
    public static RefreshToken of(UUID userId, String token, Instant expiresAt) {
        RefreshToken rt = new RefreshToken();
        rt.userId    = userId;
        rt.token     = token;
        rt.expiresAt = expiresAt;
        return rt;
    }

    /** @return {@code true} if this token has passed its expiry timestamp */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /** @return {@code true} if this token is either revoked or expired */
    public boolean isInvalid() {
        return revoked || isExpired();
    }
}
