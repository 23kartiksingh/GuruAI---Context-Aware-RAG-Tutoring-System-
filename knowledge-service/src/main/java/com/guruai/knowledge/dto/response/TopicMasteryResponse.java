package com.guruai.knowledge.dto.response;

import com.guruai.common.enums.MasteryLevel;

import java.time.Instant;
import java.util.UUID;

public record TopicMasteryResponse(
        UUID id,
        UUID userId,
        // Nullable — legacy rows from before mastery was session-scoped, and
        // the neutral placeholder MasteryServiceImpl.getTopicMastery returns
        // for a topic with no record yet, both have no session. The frontend
        // uses this to label which session a topic came from whenever the
        // same topic name shows up under more than one.
        UUID sessionId,
        String subject,
        String topic,
        double emaScore,
        int correctCount,
        int totalCount,
        MasteryLevel masteryLevel,
        Instant lastUpdated
) {}
