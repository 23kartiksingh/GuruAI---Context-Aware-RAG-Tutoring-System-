package com.guruai.knowledge.service;

import com.guruai.knowledge.dto.response.MasteryProfileResponse;
import com.guruai.knowledge.dto.response.TopicMasteryResponse;
import com.guruai.knowledge.dto.response.UserStatsResponse;

import java.util.List;
import java.util.UUID;

public interface MasteryService {

    MasteryProfileResponse getMasteryProfile(UUID userId);

    TopicMasteryResponse getTopicMastery(UUID userId, String subject, String topic);

    UserStatsResponse getUserStats(UUID userId);

    /**
     * Apply one EMA signal to the student's mastery of a topic.
     *
     * @param sessionId session the signal came from — mastery is tracked per
     *                  session, so the same topic in another session is a
     *                  separate record. May be null for signals that predate
     *                  session scoping.
     */
    void updateMastery(UUID userId, UUID sessionId, String subject, String topic, boolean correct);

    /** Remove all mastery recorded in a session (session was deleted). */
    int deleteBySession(UUID userId, UUID sessionId);

    List<TopicMasteryResponse> getWeakTopics(UUID userId);

    List<TopicMasteryResponse> getStrongTopics(UUID userId);
}
