package com.guruai.quiz.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record SubmitAnswerRequest(
        @NotNull UUID questionId,
        @NotBlank
        @Pattern(regexp = "[ABCD]", message = "Answer must be A, B, C, or D")
        String answer
) {}
