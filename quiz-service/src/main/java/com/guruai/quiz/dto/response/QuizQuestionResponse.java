package com.guruai.quiz.dto.response;

import java.util.List;
import java.util.UUID;

public record QuizQuestionResponse(
        UUID id,
        String questionText,
        List<String> options,   // ["A) ...", "B) ...", "C) ...", "D) ..."]
        String topic
        // NOTE: correctAnswer is intentionally omitted — never sent to client
) {}
