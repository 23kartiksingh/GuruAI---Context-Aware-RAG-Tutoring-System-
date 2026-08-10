package com.guruai.memory.event.consumer;

import com.guruai.common.constants.KafkaTopics;
import com.guruai.common.events.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserRegisteredConsumer {

    @KafkaListener(topics = KafkaTopics.USER_REGISTERED, groupId = "memory-service-group")
    public void onUserRegistered(UserRegisteredEvent event) {
        // Memory store is created on-demand — no pre-init needed.
        log.info("Memory service acknowledged user.registered for userId={}", event.userId());
    }
}
