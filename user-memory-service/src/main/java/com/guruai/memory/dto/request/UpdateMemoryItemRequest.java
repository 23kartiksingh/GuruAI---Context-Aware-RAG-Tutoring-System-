package com.guruai.memory.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Direct edit of an existing item's text — bypasses extraction, this IS the final text. */
public record UpdateMemoryItemRequest(
        @NotBlank(message = "Text must not be blank")
        String text
) {}
