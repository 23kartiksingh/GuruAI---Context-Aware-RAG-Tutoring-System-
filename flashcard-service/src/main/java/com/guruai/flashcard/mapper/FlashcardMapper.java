package com.guruai.flashcard.mapper;

import com.guruai.flashcard.dto.response.FlashcardResponse;
import com.guruai.flashcard.entity.Flashcard;
import org.springframework.stereotype.Component;

@Component
public class FlashcardMapper {

    public FlashcardResponse toResponse(Flashcard entity) {
        return new FlashcardResponse(
                entity.getId(), entity.getUserId(), entity.getSessionId(),
                entity.getSubject(), entity.getTopic(),
                entity.getFront(), entity.getBack(),
                entity.getEaseFactor(), entity.getIntervalDays(),
                entity.getRepetitions(), entity.getNextReviewDate(),
                entity.getCreatedAt()
        );
    }
}
