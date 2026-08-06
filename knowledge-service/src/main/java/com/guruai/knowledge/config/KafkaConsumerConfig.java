package com.guruai.knowledge.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer setup. This service listens to FOUR different event types
 * (user.registered, quiz.completed, flashcard.reviewed, chat.message.saved),
 * and our producers deliberately don't embed Java type info in message
 * headers — so the deserialization strategy matters:
 *
 * <p>Values come off the wire as plain JSON strings, and the
 * {@link StringJsonMessageConverter} on the listener factory converts each
 * one to whatever record type the {@code @KafkaListener} method declares as
 * its parameter. The target type comes from the METHOD SIGNATURE, which is
 * the only approach that works with multiple event types per service.
 *
 * <p>(The previous version configured {@code JsonDeserializer} with
 * {@code VALUE_DEFAULT_TYPE = Object.class}, which deserializes every payload
 * to a LinkedHashMap and then fails to match the typed listener parameters —
 * every listener in this service would have thrown conversion errors at
 * runtime. Same fix applied in flashcard/user-memory/notification services.)
 */
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9094}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:knowledge-service-group}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        // JSON -> listener parameter type, per method signature (see class javadoc)
        factory.setRecordMessageConverter(new StringJsonMessageConverter());
        return factory;
    }
}
