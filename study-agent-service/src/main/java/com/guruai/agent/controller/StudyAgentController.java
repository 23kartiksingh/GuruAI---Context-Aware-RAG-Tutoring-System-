package com.guruai.agent.controller;

import com.guruai.agent.dto.request.ChatRequest;
import com.guruai.agent.dto.request.CreateSessionRequest;
import com.guruai.agent.dto.response.ChatResponse;
import com.guruai.agent.dto.response.MessageResponse;
import com.guruai.agent.dto.response.SessionResponse;
import com.guruai.agent.service.SessionService;
import com.guruai.agent.service.StudyAgentService;
import com.guruai.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class StudyAgentController {

    private final StudyAgentService studyAgentService;
    private final SessionService sessionService;

    // ── Session endpoints ──────────────────────────────────────────────

    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<SessionResponse>> createSession(
            @Valid @RequestBody CreateSessionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.createSession(request)));
    }

    @GetMapping("/sessions/{userId}")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getSessions(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.getUserSessions(userId)));
    }

    /**
     * Delete a session and everything it produced.
     *
     * <p>The session and its messages go immediately; documents, flashcards
     * and mastery are removed by the owning services when they consume the
     * {@code session.deleted} event, so cleanup is eventually consistent.
     *
     * <p>{@code userId} comes from the gateway-verified {@code X-User-Id}
     * header rather than the path, so a caller can only delete their own
     * sessions.
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @PathVariable UUID sessionId,
            @RequestHeader("X-User-Id") UUID userId) {
        sessionService.deleteSession(sessionId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Session deleted"));
    }

    @GetMapping("/sessions/{sessionId}/history")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getHistory(
            @PathVariable UUID sessionId) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.getSessionHistory(sessionId)));
    }

    // ── Chat endpoints (CRAG pipeline) ─────────────────────────────────

    /** Blocking chat — whole answer in one JSON response. */
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(studyAgentService.chat(request)));
    }

    /**
     * Streaming chat — Server-Sent Events, one event per token chunk.
     * The frontend renders these incrementally ("typing" effect). Returning
     * {@code Flux<String>} with an SSE media type is all Spring needs to
     * stream; no manual SseEmitter plumbing required. The api-gateway's
     * response-timeout is raised to 5m specifically to keep this alive.
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@Valid @RequestBody ChatRequest request) {
        return studyAgentService.chatStream(request);
    }
}
