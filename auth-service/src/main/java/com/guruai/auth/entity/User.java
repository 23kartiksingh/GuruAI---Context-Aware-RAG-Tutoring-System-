package com.guruai.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapped to the {@code users} table in {@code auth_db}.
 *
 * <p>Schema managed by Flyway — see {@code V1__create_users_table.sql}.
 *
 * <p><b>Never expose this entity directly from a controller.</b>
 * Always convert to {@link com.guruai.auth.dto.response.UserProfileResponse}
 * via {@link com.guruai.auth.mapper.UserMapper}.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Unique, case-sensitive login identifier (lowercase enforced at service layer). */
    @Column(name = "username", unique = true, nullable = false, length = 100)
    private String username;

    /** BCrypt hash of the user's password. Never the raw password. */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /** Human-readable display name shown in the UI. Defaults to "The Scholar". */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** Short bio / learning goals. May be empty. */
    @Column(name = "bio", nullable = false, columnDefinition = "TEXT")
    private String bio;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    /** Called by JPA before INSERT — sets defaults. */
    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        if (name == null || name.isBlank()) {
            name = "The Scholar";
        }
        if (bio == null) {
            bio = "";
        }
    }

    // ── Convenience constructor (used in AuthServiceImpl) ─────────────────
    public User(String username, String passwordHash, String name) {
        this.username     = username;
        this.passwordHash = passwordHash;
        this.name         = (name != null && !name.isBlank()) ? name : "The Scholar";
        this.bio          = "";
    }
}
