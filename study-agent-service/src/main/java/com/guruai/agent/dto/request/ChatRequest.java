package com.guruai.agent.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChatRequest(
        @NotNull UUID userId,
        @NotNull UUID sessionId,
        @NotBlank String message
) {}
