package com.guruai.quiz.dto.response;

import com.guruai.common.enums.DifficultyLevel;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuizResponse(
        UUID quizId,
        UUID userId,
        UUID sessionId,
        String subject,
        String topic,
        DifficultyLevel difficulty,
        List<QuizQuestionResponse> questions,
        Instant createdAt
) {}
