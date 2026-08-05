package com.guruai.agent.service;

import com.guruai.agent.dto.response.MessageResponse;
import com.guruai.agent.dto.response.SessionResponse;
import com.guruai.agent.dto.request.CreateSessionRequest;

import java.util.List;
import java.util.UUID;

public interface SessionService {

    SessionResponse createSession(CreateSessionRequest request);

    List<SessionResponse> getUserSessions(UUID userId);

    List<MessageResponse> getSessionHistory(UUID sessionId);

    /**
     * Delete a session and its chat messages, then announce it on Kafka so
     * the other services drop their session-scoped data.
     *
     * @param userId owner — the delete is rejected if it doesn't match, so one
     *               user can't delete another's session by guessing an ID
     */
    void deleteSession(UUID sessionId, UUID userId);
}
