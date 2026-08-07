package com.guruai.knowledge.event.consumer;

import com.guruai.common.constants.KafkaTopics;
import com.guruai.common.events.QuizCompletedEvent;
import com.guruai.knowledge.service.MasteryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class QuizCompletedConsumer {

    private final MasteryService masteryService;

    @KafkaListener(topics = KafkaTopics.QUIZ_COMPLETED, groupId = "knowledge-service-group")
    public void onQuizCompleted(QuizCompletedEvent event) {
        log.info("Received quiz.completed userId={} subject={} topic={} correct={}",
                event.userId(), event.subject(), event.topic(), event.isCorrect());
        try {
            masteryService.updateMastery(
                    UUID.fromString(event.userId()),
                    event.sessionId() == null ? null : UUID.fromString(event.sessionId()),
                    event.subject(),
                    event.topic(),
                    event.isCorrect()
            );
        } catch (Exception e) {
            log.error("Failed to update mastery for quiz event: {}", e.getMessage(), e);
        }
    }
}
