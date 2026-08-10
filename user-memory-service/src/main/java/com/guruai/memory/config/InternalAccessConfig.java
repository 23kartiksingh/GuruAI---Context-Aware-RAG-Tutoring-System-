package com.guruai.memory.config;

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
 */
@Configuration
@EnableConfigurationProperties(InternalAccessProperties.class)
public class InternalAccessConfig {

    @Bean
    public FilterRegistrationBean<InternalAccessFilter> internalAccessFilter(
            InternalAccessProperties properties) {
        FilterRegistrationBean<InternalAccessFilter> registration =
                new FilterRegistrationBean<>(new InternalAccessFilter(properties));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
