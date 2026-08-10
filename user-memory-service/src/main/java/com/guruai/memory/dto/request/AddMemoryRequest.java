package com.guruai.memory.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AddMemoryRequest(
        @NotBlank(message = "Message must not be blank")
        String message
) {}
