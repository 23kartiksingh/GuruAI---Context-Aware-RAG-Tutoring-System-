package com.guruai.flashcard.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FlashcardResponse(
        UUID id,
        UUID userId,
        UUID sessionId,
        String subject,
        String topic,
        String front,
        String back,
        double easeFactor,
        int intervalDays,
        int repetitions,
        LocalDate nextReviewDate,
        Instant createdAt
) {}
