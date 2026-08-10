package com.guruai.flashcard.dto.response;

import java.time.LocalDate;

public record ReviewResultResponse(
        double newEaseFactor,
        int newIntervalDays,
        LocalDate nextReviewDate
) {}
