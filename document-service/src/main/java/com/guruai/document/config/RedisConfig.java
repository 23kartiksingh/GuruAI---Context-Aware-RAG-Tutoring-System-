package com.guruai.document.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for Document Service.
 *
 * <p>Redis is used as an LRU cache for query results from Hybrid Search.
 * The cache key is: {@code retriever:cache:{sessionId}:{queryHash}}.
 *
 * <p>Cache behaviour (ported from Python {@code retriever.py}):
 * <ul>
 *   <li>Max 32 entries (configured via {@code guruai.retriever.cache-max-size})</li>
 *   <li>TTL: 5 minutes (configured via {@code guruai.retriever.cache-ttl-seconds})</li>
 *   <li>LRU eviction via Redis {@code maxmemory-policy allkeys-lru}</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(DocumentProperties.class)
public class RedisConfig {

    /**
     * RedisTemplate for caching {@code List<ChunkResponse>} objects.
     * Uses Jackson JSON serialisation for human-readable inspection via Redis Commander.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
