package com.guruai.flashcard.service;

import com.guruai.flashcard.dto.response.FlashcardResponse;
import com.guruai.flashcard.dto.response.ReviewResultResponse;

import java.util.List;
import java.util.UUID;

public interface FlashcardService {

    List<FlashcardResponse> getDueToday(UUID userId);

    long getDueTodayCount(UUID userId);

    ReviewResultResponse review(UUID cardId, int quality);

    List<FlashcardResponse> getByUser(UUID userId);

    void generateForSession(UUID userId, UUID sessionId, String subject,
                            String topic, String chunkText);

    /**
     * Delete every flashcard belonging to a session.
     * Triggered by {@code session.deleted}.
     *
     * @return how many cards were removed
     */
    int deleteBySession(UUID userId, UUID sessionId);
}
