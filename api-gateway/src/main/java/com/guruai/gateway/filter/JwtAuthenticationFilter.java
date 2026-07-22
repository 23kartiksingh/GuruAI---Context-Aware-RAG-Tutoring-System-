package com.guruai.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.guruai.common.dto.ApiResponse;
import com.guruai.common.security.InternalAccessProperties;
import com.guruai.gateway.config.GatewaySecurityProperties;
import com.guruai.gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * The gateway's front door: every request that isn't on the public-path
 * allowlist must carry a valid access token, or it never reaches a
 * backend service.
 *
 * <p>This mirrors auth-service's own {@code JwtAuthFilter} (same cookie
 * name, same header fallback, same Redis blacklist key pattern) because
 * both sides need to agree on how a token is found and judged valid —
 * see {@link com.guruai.gateway.config.JwtProperties} for the shared-secret
 * requirement.
 *
 * <p>On success, this filter sets {@code X-User-Id} (and {@code X-User-Username})
 * on the request before it's forwarded downstream. Every backend service
 * trusts that header instead of re-verifying the JWT itself — which is only
 * safe because this filter always strips any client-supplied value for that
 * header first, so a caller can never forge it by just setting it themselves.
 *
 * <p>Separately, EVERY forwarded request (public path or not) also gets an
 * {@code X-Internal-Secret} header stamped on it — that's what lets each
 * downstream service's {@code InternalAccessFilter} (in common-lib) confirm
 * a request actually came through this gateway rather than hitting the
 * service's port directly.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String COOKIE_NAME          = "access_token";
    private static final String BLACKLIST_PREFIX     = "blacklist:jti:";
    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final SecretKey signingKey;
    private final GatewaySecurityProperties securityProperties;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String internalSecret;

    public JwtAuthenticationFilter(JwtProperties jwtProperties,
                                    GatewaySecurityProperties securityProperties,
                                    InternalAccessProperties internalAccessProperties,
                                    ReactiveStringRedisTemplate redisTemplate) {
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.securityProperties = securityProperties;
        this.internalSecret = internalAccessProperties.secret();
        this.redisTemplate = redisTemplate;
        // Built here rather than injected: Boot 4's reactive stack doesn't
        // auto-configure a classic com.fasterxml ObjectMapper bean, and this
        // filter only serializes one small error record anyway. JavaTimeModule
        // is needed for ApiResponse's Instant timestamp field.
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public int getOrder() {
        // Run early — before routing and before the RequestRateLimiter default
        // filter — so an unauthenticated request never reaches a real backend
        // or eats into anyone's rate-limit budget.
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isPublicPath(path)) {
            // Still stamp the internal secret — auth-service's own
            // InternalAccessFilter checks it on /auth/login and /auth/register
            // too, even though no JWT exists yet at that point.
            return chain.filter(exchange.mutate()
                    .request(stampInternalSecret(request))
                    .build());
        }

        String token = extractToken(request);
        if (token == null) {
            return reject(exchange, "Missing authentication token");
        }

        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(signingKey).build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.debug("Rejected invalid/expired token on {}: {}", path, e.getMessage());
            return reject(exchange, "Invalid or expired token");
        }

        // Only "access" tokens may be used to call the API — a refresh token
        // is only ever valid at POST /auth/refresh, which auth-service checks
        // against its own DB, not this claim.
        if (!"access".equals(claims.get("type", String.class))) {
            return reject(exchange, "Invalid token type");
        }

        return redisTemplate.hasKey(BLACKLIST_PREFIX + claims.getId())
                .defaultIfEmpty(false)
                .flatMap(blacklisted -> {
                    if (Boolean.TRUE.equals(blacklisted)) {
                        return reject(exchange, "Token has been revoked");
                    }
                    return chain.filter(exchange.mutate()
                            .request(injectUserHeaders(request, claims))
                            .build());
                });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isPublicPath(String path) {
        return securityProperties.publicPaths().stream()
                .anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    /**
     * Cookie first (set by auth-service on login), then Authorization header
     * (API clients / Postman / the eventual React frontend before it adopts
     * the cookie flow) — same precedence as auth-service's own filter.
     */
    private String extractToken(ServerHttpRequest request) {
        HttpCookie cookie = request.getCookies().getFirst(COOKIE_NAME);
        if (cookie != null && !cookie.getValue().isBlank()) {
            return cookie.getValue();
        }
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    /**
     * Strip any client-supplied X-User-Id/X-User-Username/X-Internal-Secret
     * first, then set our own — this is what makes it safe for downstream
     * services to trust these headers without re-verifying the JWT themselves.
     */
    private ServerHttpRequest injectUserHeaders(ServerHttpRequest request, Claims claims) {
        String username = claims.get("username", String.class);
        return request.mutate()
                .headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-User-Username");
                    headers.set("X-User-Id", claims.getSubject());
                    if (username != null) {
                        headers.set("X-User-Username", username);
                    }
                    headers.remove(INTERNAL_SECRET_HEADER);
                    headers.set(INTERNAL_SECRET_HEADER, internalSecret);
                })
                .build();
    }

    /** Same anti-spoofing strip-then-set, used on the public-path passthrough. */
    private ServerHttpRequest stampInternalSecret(ServerHttpRequest request) {
        return request.mutate()
                .headers(headers -> {
                    headers.remove(INTERNAL_SECRET_HEADER);
                    headers.set(INTERNAL_SECRET_HEADER, internalSecret);
                })
                .build();
    }

    private Mono<Void> reject(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(ApiResponse.unauthorized(message));
        } catch (Exception e) {
            // Should never happen (ApiResponse is a plain record), but don't let a
            // serialization failure turn a clean 401 into an ugly 500.
            body = ("{\"success\":false,\"status\":401,\"error\":\"" + message + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }
}
