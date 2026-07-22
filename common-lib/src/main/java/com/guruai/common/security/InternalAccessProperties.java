package com.guruai.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binding for {@code guruai.internal.secret} — the shared value every
 * downstream service checks for on the {@code X-Internal-Secret} header
 * before trusting anything else on the request (see {@link InternalAccessFilter}).
 *
 * <p>Not as strict a length requirement as {@code guruai.jwt.secret} (this
 * isn't signing anything, just being compared), but it still needs to be
 * long enough that guessing it isn't realistic.
 *
 * @param secret shared value, must match api-gateway's {@code INTERNAL_SERVICE_SECRET}
 */
@ConfigurationProperties(prefix = "guruai.internal")
public record InternalAccessProperties(String secret) {

    public InternalAccessProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "guruai.internal.secret must be at least 32 characters and must match " +
                    "the INTERNAL_SERVICE_SECRET the api-gateway uses — this is the value " +
                    "that proves a request actually came through the gateway.");
        }
    }
}
