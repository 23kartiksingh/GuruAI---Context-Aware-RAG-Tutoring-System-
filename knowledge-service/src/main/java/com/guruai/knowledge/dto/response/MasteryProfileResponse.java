package com.guruai.knowledge.dto.response;

import java.util.List;
import java.util.UUID;

public record MasteryProfileResponse(
        UUID userId,
        List<TopicMasteryResponse> topics,
        int totalTopics,
        int weakCount,
        int averageCount,
        int strongCount,
        double overallMasteryPct
) {}
