package com.guruai.agent.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ChatResponse(
        UUID messageId,
        String reply,
        boolean usedRag,      // true if answer was grounded by vector context
        Instant timestamp
) {}
