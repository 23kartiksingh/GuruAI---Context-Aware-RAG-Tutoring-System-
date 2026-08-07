package com.guruai.knowledge.event.consumer;

import com.guruai.common.constants.KafkaTopics;
import com.guruai.common.events.UserRegisteredEvent;
import com.guruai.knowledge.service.SubjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserRegisteredConsumer {

    private final SubjectService subjectService;

    @KafkaListener(topics = KafkaTopics.USER_REGISTERED, groupId = "knowledge-service-group")
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Received user.registered for userId={}", event.userId());
        subjectService.initializeUserProfile(java.util.UUID.fromString(event.userId()));
    }
}
