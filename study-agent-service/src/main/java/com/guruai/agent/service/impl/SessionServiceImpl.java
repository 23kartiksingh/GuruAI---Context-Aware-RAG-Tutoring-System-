package com.guruai.agent.service.impl;

import com.guruai.agent.dto.request.CreateSessionRequest;
import com.guruai.agent.dto.response.MessageResponse;
import com.guruai.agent.dto.response.SessionResponse;
import com.guruai.agent.entity.Session;
import com.guruai.agent.repository.MessageRepository;
import com.guruai.agent.repository.SessionRepository;
import com.guruai.agent.event.producer.AgentEventProducer;
import com.guruai.agent.service.SessionService;
import com.guruai.common.events.SessionDeletedEvent;
import com.guruai.common.exception.GuruAIException;
import com.guruai.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;
    private final MessageRepository messageRepository;
    private final AgentEventProducer eventProducer;

    @Override
    @Transactional
    public SessionResponse createSession(CreateSessionRequest request) {
        Session session = new Session(request.userId(), request.title(), request.subject());
        session = sessionRepository.save(session);
        return toResponse(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionResponse> getUserSessions(UUID userId) {
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getSessionHistory(UUID sessionId) {
        sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .map(m -> new MessageResponse(m.getId(), m.getRole(),
                        m.getContent(), m.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional
    public void deleteSession(UUID sessionId, UUID userId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        // Ownership check — without it, knowing a session UUID would be enough
        // to delete someone else's session and everything attached to it.
        if (!session.getUserId().equals(userId)) {
            throw new GuruAIException(HttpStatus.FORBIDDEN, "This session belongs to another user.");
        }

        messageRepository.deleteAll(messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId));
        sessionRepository.delete(session);

        // Everything else the session accumulated (documents + their pgvector
        // chunks, flashcards, mastery) lives in other services. Publish once
        // and let each clean up what it owns.
        eventProducer.publishSessionDeleted(
                SessionDeletedEvent.of(sessionId.toString(), userId.toString()));

        log.info("Deleted session {} for userId={}", sessionId, userId);
    }

    private SessionResponse toResponse(Session s) {
        return new SessionResponse(s.getId(), s.getUserId(), s.getTitle(),
                s.getSubject(), s.getCreatedAt(), s.getUpdatedAt());
    }
}
