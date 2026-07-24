package com.guruai.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed binding for {@code guruai.cookie.*} in application.yml.
 *
 * @param secure   whether to set the Secure flag on Set-Cookie headers
 *                 (must be {@code true} in production behind HTTPS)
 * @param sameSite SameSite cookie attribute ("Strict" | "Lax" | "None")
 */
@ConfigurationProperties(prefix = "guruai.cookie")
public record CookieProperties(
        boolean secure,
        String  sameSite
) {}
