package com.guruai.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for the Auth Service.
 *
 * <p>Redis is used for two purposes in auth-service:
 * <ol>
 *   <li><b>JWT Blacklist</b> — On logout, the access token's JTI is stored with
 *       a TTL equal to the token's remaining lifetime. The {@link JwtAuthFilter}
 *       rejects any token whose JTI is in the blacklist.
 *       Key pattern: {@code blacklist:jti:{jti}}</li>
 *   <li><b>Refresh Token Rate Limit</b> (optional future enhancement) —
 *       Track refresh attempts per user to prevent refresh token abuse.</li>
 * </ol>
 */
@Configuration
public class RedisConfig {

    /**
     * {@link StringRedisTemplate} — simple key/value Redis client.
     * Used by {@link JwtAuthFilter} for blacklist lookups and
     * {@link com.guruai.auth.service.impl.TokenServiceImpl} for blacklist writes.
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
