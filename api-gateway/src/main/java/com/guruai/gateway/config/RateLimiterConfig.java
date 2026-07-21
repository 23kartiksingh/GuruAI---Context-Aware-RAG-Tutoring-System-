package com.guruai.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Backs the {@code RequestRateLimiter} filter (see application.yml's
 * {@code default-filters}), which uses Spring Cloud Gateway's built-in
 * {@code RedisRateLimiter} — a Redis-backed token-bucket limiter that's
 * auto-configured once {@code spring-boot-starter-data-redis-reactive} is
 * on the classpath. This class only supplies the {@link KeyResolver}: the
 * thing that decides WHOSE bucket a request counts against.
 */
@Configuration
public class RateLimiterConfig {

    /**
     * Rate-limit per authenticated user when we know who they are (the JWT
     * filter runs before this and sets X-User-Id), otherwise fall back to
     * their IP address — this covers unauthenticated traffic like
     * POST /auth/login, which the JWT filter lets through without a user id.
     */
    @Bean
    public KeyResolver userOrIpKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just(userId);
            }
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just(ip);
        };
    }
}
