package com.guruai.flashcard.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReviewRequest(
        @NotNull
        @Min(value = 0, message = "Quality must be between 0 and 5")
        @Max(value = 5, message = "Quality must be between 0 and 5")
        Integer quality
) {}
