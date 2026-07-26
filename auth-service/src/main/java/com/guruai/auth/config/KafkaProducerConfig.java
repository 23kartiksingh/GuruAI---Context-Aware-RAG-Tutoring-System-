package com.guruai.auth.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

/**
 * Kafka producer configuration for the Auth Service.
 *
 * <p>Publishes one event type:
 * <ul>
 *   <li>{@code user.registered} — {@link com.guruai.common.events.UserRegisteredEvent}</li>
 * </ul>
 *
 * <p>The producer uses JSON serialization so events are human-readable in Kafka UI.
 * Type headers are disabled — consumers use explicit type bindings.
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        return new DefaultKafkaProducerFactory<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,   StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class,
                // Reliability settings — wait for all replicas to ack
                ProducerConfig.ACKS_CONFIG,               "all",
                ProducerConfig.RETRIES_CONFIG,            3,
                ProducerConfig.RETRY_BACKOFF_MS_CONFIG,   500,
                // Idempotent producer — prevents duplicate messages on retry
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true,
                // No type headers — consumers use explicit type mappings
                JsonSerializer.ADD_TYPE_INFO_HEADERS,     false
        ));
    }

    /**
     * Generic {@link KafkaTemplate} — used by {@link com.guruai.auth.event.producer.AuthEventProducer}.
     * Value type is {@link Object} to allow sending any event record.
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> pf) {
        return new KafkaTemplate<>(pf);
    }
}
