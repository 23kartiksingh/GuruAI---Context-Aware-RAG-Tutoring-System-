package com.guruai.agent.service;

import com.guruai.agent.dto.request.ChatRequest;
import com.guruai.agent.dto.response.ChatResponse;
import reactor.core.publisher.Flux;

public interface StudyAgentService {

    /**
     * Core chat pipeline (blocking, full answer in one response):
     * <ol>
     *   <li>CRAG — retrieve chunks from document-service, grade relevance,
     *       fall back to general knowledge when nothing survives</li>
     *   <li>Fetch user memory context for personalisation</li>
     *   <li>Call the LLM with {@code AgentTools} registered (the model may
     *       call back into knowledge-service / document-service mid-turn)</li>
     *   <li>Persist both message turns to agent_db</li>
     *   <li>Publish {@code chat.message.saved} to Kafka for each turn</li>
     * </ol>
     */
    ChatResponse chat(ChatRequest request);

    /**
     * Same pipeline as {@link #chat}, but returns the answer as an SSE token
     * stream (what the React frontend renders as "typing"). Persistence and
     * Kafka events fire once the stream completes, using the fully
     * accumulated reply text.
     */
    Flux<String> chatStream(ChatRequest request);
}
