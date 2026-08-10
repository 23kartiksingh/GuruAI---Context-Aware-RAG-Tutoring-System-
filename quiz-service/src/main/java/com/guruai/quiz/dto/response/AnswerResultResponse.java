package com.guruai.quiz.dto.response;

public record AnswerResultResponse(
        boolean correct,
        String correctAnswer,
        String explanation
) {}
