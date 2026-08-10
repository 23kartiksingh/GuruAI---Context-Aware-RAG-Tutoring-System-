package com.guruai.quiz.event.producer;

import com.guruai.common.constants.KafkaTopics;
import com.guruai.common.events.QuizCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class QuizEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishQuizCompleted(QuizCompletedEvent event) {
        kafkaTemplate.send(KafkaTopics.QUIZ_COMPLETED, event.userId(), event);
        log.info("Published quiz.completed quizId={} userId={} score={}",
                event.quizId(), event.userId(), event.totalScore());
    }
}
