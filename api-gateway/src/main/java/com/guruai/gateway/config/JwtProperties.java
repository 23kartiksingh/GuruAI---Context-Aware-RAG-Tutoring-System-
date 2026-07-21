package com.guruai.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed binding for {@code guruai.jwt.secret}.
 *
 * <p>The gateway never <i>issues</i> tokens (that's auth-service's job) —
 * it only verifies signatures, so it only needs the shared secret, not the
 * expiry settings auth-service also binds.
 *
 * <p>Both services must be given the exact same {@code JWT_SECRET} env var,
 * otherwise every token the gateway sees will fail signature verification.
 *
 * @param secret raw secret string (min 64 chars — required for HS512)
 */
@ConfigurationProperties(prefix = "guruai.jwt")
public record JwtProperties(String secret) {

    /** Fail fast on startup if the secret is missing or too short for HS512. */
    public JwtProperties {
        if (secret == null || secret.length() < 64) {
            throw new IllegalStateException(
                    "guruai.jwt.secret must be at least 64 characters long (HS512 requirement) " +
                    "and must match the JWT_SECRET auth-service uses to sign tokens.");
        }
    }
}
