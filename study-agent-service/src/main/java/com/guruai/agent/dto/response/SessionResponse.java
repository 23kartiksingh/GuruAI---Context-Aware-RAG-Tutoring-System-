package com.guruai.agent.dto.response;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        UUID userId,
        String title,
        String subject,
        Instant createdAt,
        Instant updatedAt
) {}
