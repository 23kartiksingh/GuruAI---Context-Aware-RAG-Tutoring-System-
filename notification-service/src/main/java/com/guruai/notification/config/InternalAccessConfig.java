package com.guruai.notification.config;

import com.guruai.common.security.InternalAccessFilter;
import com.guruai.common.security.InternalAccessProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registers {@link InternalAccessFilter} so this service rejects any
 * request that didn't come through api-gateway — see that class for why
 * this matters. One of these lives in every downstream service.
 *
 * <p>Note: this deliberately does NOT cover the raw WebSocket/SockJS
 * handshake at {@code /ws/**} (see {@link WebSocketConfig}) — STOMP
 * connections aren't simple request/response, so header-based checks
 * don't apply the same way. The gateway's {@code /ws/**} route still
 * requires a valid session to reach this service in the first place.
 */
@Configuration
@EnableConfigurationProperties(InternalAccessProperties.class)
public class InternalAccessConfig {

    @Bean
    public FilterRegistrationBean<InternalAccessFilter> internalAccessFilter(
            InternalAccessProperties properties) {
        FilterRegistrationBean<InternalAccessFilter> registration =
                new FilterRegistrationBean<>(new InternalAccessFilter(properties));
        // Exclude /ws/** — filtering a servlet Filter on a WebSocket upgrade
        // request is unreliable across containers, and this service's REST
        // endpoints (/notifications/**) are still fully protected.
        registration.addUrlPatterns("/notifications/*", "/actuator/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
