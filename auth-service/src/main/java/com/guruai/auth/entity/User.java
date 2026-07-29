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

    /** BCrypt hash of the user's password. Never the raw password. NULL for Google-only accounts. */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    /** Human-readable display name shown in the UI. Defaults to "The Scholar". */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** Short bio / learning goals. May be empty. */
    @Column(name = "bio", nullable = false, columnDefinition = "TEXT")
    private String bio;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    /** Verified email from the OAuth2 provider. NULL for LOCAL accounts (register never asks for one). */
    @Column(name = "email", length = 255)
    private String email;

    /** {@code LOCAL} (username/password) or {@code GOOGLE}. Defaults to LOCAL. */
    @Column(name = "auth_provider", nullable = false, length = 20)
    private String authProvider;

    /** Provider's stable subject id (Google's {@code sub} claim). NULL for LOCAL accounts. */
    @Column(name = "provider_id", length = 255)
    private String providerId;

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
        if (authProvider == null || authProvider.isBlank()) {
            authProvider = "LOCAL";
        }
    }

    // ── Convenience constructor (used in AuthServiceImpl for password registration) ──
    public User(String username, String passwordHash, String name) {
        this.username     = username;
        this.passwordHash = passwordHash;
        this.name         = (name != null && !name.isBlank()) ? name : "The Scholar";
        this.bio          = "";
        this.authProvider = "LOCAL";
    }

    /**
     * Build a new user backed by a Google account — no password, matched on
     * (authProvider, providerId) on future logins rather than credentials.
     *
     * @param username   generated from the Google email's local part (unique — caller's job)
     * @param email      verified email from Google's userinfo response
     * @param name       display name from Google's profile ("The Scholar" if blank)
     * @param providerId Google's stable "sub" claim
     */
    public static User forGoogleSignup(String username, String email, String name, String providerId) {
        User user = new User();
        user.username     = username;
        user.email        = email;
        user.name         = (name != null && !name.isBlank()) ? name : "The Scholar";
        user.bio          = "";
        user.authProvider = "GOOGLE";
        user.providerId   = providerId;
        return user;
    }
}
