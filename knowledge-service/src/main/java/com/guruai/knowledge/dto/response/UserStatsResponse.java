package com.guruai.knowledge.dto.response;

import java.util.UUID;

public record UserStatsResponse(
        UUID userId,
        int totalQuestions,
        int correctAnswers,
        double avgMasteryPct
) {}
