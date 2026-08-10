package com.guruai.flashcard.event.producer;

import com.guruai.common.constants.KafkaTopics;
import com.guruai.common.events.FlashcardReviewedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FlashcardEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishFlashcardReviewed(FlashcardReviewedEvent event) {
        kafkaTemplate.send(KafkaTopics.FLASHCARD_REVIEWED, event.userId(), event);
        log.info("Published flashcard.reviewed cardId={} userId={} quality={}",
                event.cardId(), event.userId(), event.quality());
    }
}
