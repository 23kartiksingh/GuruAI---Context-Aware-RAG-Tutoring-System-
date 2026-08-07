package com.guruai.knowledge.event.producer;

import com.guruai.common.constants.KafkaTopics;
import com.guruai.common.events.MasteryDroppedEvent;
import com.guruai.common.events.WeakTopicReminderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishMasteryDropped(MasteryDroppedEvent event) {
        kafkaTemplate.send(KafkaTopics.MASTERY_DROPPED, event.userId(), event);
        log.info("Published mastery.dropped for userId={} topic={} score={}",
                event.userId(), event.topic(), event.newEmaScore());
    }

    public void publishWeakTopicReminder(WeakTopicReminderEvent event) {
        kafkaTemplate.send(KafkaTopics.WEAK_TOPIC_REMINDER, event.userId(), event);
        log.info("Published weak.topic.reminder for userId={} topic={} sessionId={}",
                event.userId(), event.topic(), event.sessionId());
    }
}
