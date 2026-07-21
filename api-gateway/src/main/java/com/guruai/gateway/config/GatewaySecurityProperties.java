package com.guruai.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Binding for {@code guruai.gateway.public-paths} — the list of Ant-style
 * path patterns that are allowed through {@link com.guruai.gateway.filter.JwtAuthenticationFilter}
 * WITHOUT a valid JWT (registration, login, token refresh, health checks).
 *
 * <p>Kept in config rather than hard-coded so new public endpoints can be
 * added without touching Java code.
 *
 * @param publicPaths Ant-style patterns (e.g. {@code /auth/login}) matched
 *                     against the incoming request path
 */
@ConfigurationProperties(prefix = "guruai.gateway")
public record GatewaySecurityProperties(List<String> publicPaths) {

    public GatewaySecurityProperties {
        if (publicPaths == null) {
            publicPaths = List.of();
        }
    }
}
