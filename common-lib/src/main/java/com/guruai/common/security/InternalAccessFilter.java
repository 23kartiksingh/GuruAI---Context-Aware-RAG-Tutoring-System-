package com.guruai.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.guruai.common.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Closes the "direct access" loophole: without this, anyone who could reach
 * a service's port directly — bypassing api-gateway entirely — could set
 * {@code X-User-Id} themselves and impersonate any user, since every
 * controller in this project trusts that header at face value (by design —
 * see api-gateway's JwtAuthenticationFilter, which is the ONLY thing meant
 * to ever set it).
 *
 * <p>Every request must carry {@code X-Internal-Secret} matching this
 * service's {@code guruai.internal.secret}, or it's rejected before it
 * reaches any controller. Api-gateway stamps this header on every request
 * it proxies (see its JwtAuthenticationFilter), so normal traffic through
 * the gateway is unaffected — this only blocks callers who skip the gateway.
 *
 * <p>This is a second layer on top of the network-level fix in
 * docker-compose.yml (downstream services no longer publish host ports at
 * all in the containerized stack) — defense in depth, since the network
 * restriction alone wouldn't help if these services were ever deployed
 * somewhere that doesn't isolate them the same way.
 *
 * <p>{@code /actuator/**} is exempt so container healthchecks (which call
 * the service directly, not through the gateway) keep working.
 */
public class InternalAccessFilter extends OncePerRequestFilter {

    private static final String SECRET_HEADER = "X-Internal-Secret";

    private final byte[] expectedSecretBytes;
    private final ObjectMapper objectMapper;

    public InternalAccessFilter(InternalAccessProperties properties) {
        this.expectedSecretBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        String provided = request.getHeader(SECRET_HEADER);
        if (provided == null || !MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8), expectedSecretBytes)) {
            // Constant-time comparison — a naive .equals() would leak how many
            // leading characters matched via response timing, which matters
            // for a shared secret exactly as much as it does for a password.
            respondForbidden(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void respondForbidden(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.forbidden("This service only accepts requests routed through the API gateway")));
    }
}
