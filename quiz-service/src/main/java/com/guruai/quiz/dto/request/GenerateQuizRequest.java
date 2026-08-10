package com.guruai.quiz.dto.request;

import com.guruai.common.enums.DifficultyLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


import java.util.UUID;

/**
 * @param topic      optional — if null, picks the weakest topic
 * @param difficulty optional — null means "Auto": quiz-service derives the
 *                   difficulty from the student's current mastery of this
 *                   subject/topic via knowledge-service
 */
public record GenerateQuizRequest(
        @NotNull UUID userId,
        @NotNull UUID sessionId,
        @NotBlank String subject,
        String topic,
        DifficultyLevel difficulty,
        @Min(1) @Max(15) int questionCount
) {}
