package com.guruai.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed binding for {@code guruai.jwt.*} in application.yml.
 *
 * <p>Registered via {@code @EnableConfigurationProperties(JwtProperties.class)}
 * in {@link SecurityConfig}.
 *
 * @param secret          raw secret string (min 64 chars for HS512)
 * @param accessExpiryMs  access token lifetime in ms   (default 3 600 000 = 1 h)
 * @param refreshExpiryMs refresh token lifetime in ms  (default 1 209 600 000 = 14 d)
 */
@ConfigurationProperties(prefix = "guruai.jwt")
public record JwtProperties(
        String secret,
        long   accessExpiryMs,
        long   refreshExpiryMs
) {
    /**
     * Fail fast on startup if the secret is too short — prevents weak signing keys.
     * HS512 requires a minimum of 64 bytes (512 bits).
     */
    public JwtProperties {
        if (secret == null || secret.length() < 64) {
            throw new IllegalStateException(
                    "guruai.jwt.secret must be at least 64 characters long (HS512 requirement). " +
                    "Generate with: openssl rand -base64 64");
        }
    }
}
