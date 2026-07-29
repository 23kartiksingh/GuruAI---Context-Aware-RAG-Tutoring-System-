package com.guruai.auth.event.producer;

import com.guruai.common.constants.KafkaTopics;
import com.guruai.common.events.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Publishes auth-domain Kafka events.
 *
 * <p>Currently publishes:
 * <ul>
 *   <li>{@code user.registered} — when a new account is created</li>
 * </ul>
 *
 * <p>All methods are {@code @Async} — publishing failures do NOT roll back
 * the registration transaction. If Kafka is unavailable, the event is lost
 * (acceptable for notifications; critical events should use an Outbox pattern in future).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish a {@link UserRegisteredEvent} to the {@code user.registered} Kafka topic.
     *
     * <p>The message key is the {@code userId} — this ensures all events for the same
     * user land on the same partition (ordering guarantee per user).
     *
     * @param userId   UUID of the newly registered user
     * @param username login name
     * @param name     display name
     */
    @Async
    public void publishUserRegistered(String userId, String username, String name) {
        UserRegisteredEvent event = UserRegisteredEvent.of(userId, username, name);

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(KafkaTopics.USER_REGISTERED, userId, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish UserRegisteredEvent for userId={}: {}",
                          userId, ex.getMessage());
            } else {
                log.info("Published UserRegisteredEvent for userId={} to partition={}",
                         userId,
                         result.getRecordMetadata().partition());
            }
        });
    }
}
