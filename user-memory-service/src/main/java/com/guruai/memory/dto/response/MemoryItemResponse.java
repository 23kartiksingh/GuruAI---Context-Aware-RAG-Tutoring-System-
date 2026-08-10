package com.guruai.memory.dto.response;

import java.util.UUID;

/** One stored preference, with its id so the frontend can target it for edit/delete. */
public record MemoryItemResponse(
        UUID id,
        String text
) {}
