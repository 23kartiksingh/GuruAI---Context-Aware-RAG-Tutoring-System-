package com.guruai.agent.service.impl;

import com.guruai.agent.client.KnowledgeServiceClient;
import com.guruai.agent.client.MemoryServiceClient;
import com.guruai.agent.dto.request.ChatRequest;
import com.guruai.agent.dto.response.ChatResponse;
import com.guruai.agent.entity.Message;
import com.guruai.agent.entity.Session;
import com.guruai.agent.event.producer.AgentEventProducer;
import com.guruai.agent.repository.MessageRepository;
import com.guruai.agent.repository.SessionRepository;
import com.guruai.agent.service.CragService;
import com.guruai.agent.service.StudyAgentService;
import com.guruai.agent.tool.AgentTools;
import com.guruai.agent.tool.ToolCallGuard;
import com.guruai.common.events.ChatMessageSavedEvent;
import com.guruai.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

/**
 * The "AI Brain": CRAG-grounded, memory-personalised, tool-calling tutor.
 *
 * <p>Pipeline per turn (see {@link StudyAgentService} for the summary):
 * CRAG context + user memory + recent history go into the prompt;
 * {@link AgentTools} is registered so the model can fetch mastery data or
 * re-search documents mid-turn; both message turns are persisted and
 * announced on Kafka afterwards.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StudyAgentServiceImpl implements StudyAgentService {

    // Was 12_000 (~3000 tokens) sent on EVERY turn regardless of relevance —
    // a fixed cost that alone ate half of Groq's free-tier 6000 TPM budget
    // before the system prompt, CRAG context, or tool calls even started.
    // 4000 chars (~1000 tokens) still covers several recent turns, which is
    // what actually matters for conversational continuity.
    private static final int HISTORY_LIMIT_CHARS = 4_000;

    private final ChatClient chatClient;
    private final CragService cragService;
    private final MemoryServiceClient memoryClient;
    private final KnowledgeServiceClient knowledgeClient;
    private final SessionRepository sessionRepository;
    private final MessageRepository messageRepository;
    private final AgentEventProducer eventProducer;
    private final AgentTools agentTools;

    private static final String SYSTEM_TEMPLATE = """
            You are GuruAI, an expert AI tutor specialising in adaptive learning.

            Current student userId: %s
            Current sessionId: %s
            (Use these values when calling tools.)

            %s

            %s

            %s

            Rules:
            - Answer from the provided context whenever it is sufficient.
            - If the context is empty or insufficient, answer from your general knowledge
              and open with ONE short sentence saying it isn't from their documents.
              State this only once — never repeat the disclaimer later in the answer.
            - When the student's preferences above name something SPECIFIC (a favorite show,
              game, team, tool, language, etc.) and you're giving an example or analogy, use
              THAT SPECIFIC thing by name — not a different, more generic example from the
              same category. If they said their favorite anime is "Hunter x Hunter", an
              example should reference Hunter x Hunter itself, not some other popular anime
              that happens to also fit. Generic defaults only when nothing specific was given.
            - You have tools available: check the student's mastery profile or weak topics
              when they ask what to study or how they're doing, and search their documents
              when they reference material not already in the context. Call each tool AT
              MOST ONCE per turn — its result doesn't change if you call it again, and
              re-calling it wastes time. Once you've called what you need, answer.
            - Use markdown formatting for code, lists, and formulas.
            - End your answer with a line: "Source: %s"
            """;

    /**
     * How thoroughly to explain, based on the student's mastery of the topic
     * they're asking about. This is the "adaptive" half of adaptive tutoring:
     * the same question gets a different depth of answer depending on whether
     * the student is still building fundamentals or already fluent.
     *
     * <p>Injected deterministically rather than left to the model's judgement —
     * the agent has a mastery tool, but it only calls it when it decides to, so
     * response depth was in practice never actually adapting.
     */
    private static String depthDirective(KnowledgeServiceClient.MasteryHint hint) {
        if (hint == null || hint.level() == null) {
            return """
                   Student mastery for this topic: unknown (no quiz history yet).
                   Explain at a normal level: cover the core idea, then one concrete example.""";
        }
        String scope = hint.matchedTopic() != null
                ? "on '" + hint.matchedTopic() + "'"
                : "overall (no per-topic record for this question yet)";
        return switch (hint.level()) {
            case "WEAK" -> String.format("""
                    Student mastery %s: WEAK (%d%%).
                    They are still building fundamentals here, so explain THOROUGHLY:
                    define any term you use, go step by step, give a concrete worked
                    example and a plain-language analogy, and finish with a one-line
                    recap. Do not assume prior knowledge. Do not rush.""", scope, hint.pct());
            case "STRONG" -> String.format("""
                    Student mastery %s: STRONG (%d%%).
                    They already know this well, so be CRISP: answer directly in a few
                    sentences, skip fundamentals and basic definitions, and only add
                    detail for genuine edge cases or nuance. No hand-holding, no
                    re-explaining basics they have already demonstrated.""", scope, hint.pct());
            default -> String.format("""
                    Student mastery %s: AVERAGE (%d%%).
                    They know the basics but not the details, so pitch it MODERATELY:
                    assume the fundamentals, focus on the part they are likely missing,
                    and include one clarifying example. Keep it reasonably concise.""",
                    scope, hint.pct());
        };
    }

    // ── Blocking chat ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ChatResponse chat(ChatRequest request) {
        // Reset the per-turn duplicate-tool-call guard — this blocking path
        // keeps the WHOLE tool-calling loop (every Groq round-trip and every
        // tool execution below) on this one thread, so ThreadLocal scoping
        // works cleanly here. Always cleared in finally so a pooled thread
        // never carries state into a later, unrelated request.
        ToolCallGuard.reset();
        try {
            PreparedPrompt p = prepare(request);

            String reply = callWithRateLimitRetry(() -> chatClient.prompt()
                    .system(p.systemPrompt())
                    .user(p.userPrompt())
                    .tools(agentTools)
                    .call()
                    .content());

            Message assistantMsg = persistTurns(p.session(), request, reply);

            log.info("Chat: sessionId={} usedRag={} replyLength={}",
                     request.sessionId(), p.usedRag(), reply == null ? 0 : reply.length());

            return new ChatResponse(assistantMsg.getId(), reply, p.usedRag(), Instant.now());
        } finally {
            ToolCallGuard.clear();
        }
    }

    // ── Streaming chat (SSE) ──────────────────────────────────────────────────

    @Override
    public Flux<String> chatStream(ChatRequest request) {
        // Prepared OUTSIDE the flux so retrieval/grading errors surface
        // immediately as a normal HTTP error, not mid-stream.
        PreparedPrompt p = prepare(request);

        StringBuilder fullReply = new StringBuilder();

        return chatClient.prompt()
                .system(p.systemPrompt())
                .user(p.userPrompt())
                .tools(agentTools)
                .stream()
                .content()
                .doOnNext(fullReply::append)
                // Persist + publish only after the model finishes talking —
                // doing it per-token would be one DB write per word.
                .doOnComplete(() -> {
                    persistTurns(p.session(), request, fullReply.toString());
                    log.info("Chat stream complete: sessionId={} usedRag={} replyLength={}",
                             request.sessionId(), p.usedRag(), fullReply.length());
                });
    }

    // ── Rate-limit retry ─────────────────────────────────────────────────────

    private static final int RATE_LIMIT_MAX_RETRIES = 2;
    private static final long RATE_LIMIT_BACKOFF_MS = 4_000;

    /**
     * Retries once or twice, with backoff, specifically for Groq TPM
     * rate-limit failures (HTTP 413/429 — "Request too large ... tokens per
     * minute"). Tool-calling turns can make several sequential model calls
     * within a few seconds; occasionally that still bursts past the
     * free-tier budget even after trimming what gets sent. Groq's TPM window
     * resets every 60s, so a short wait-and-retry recovers automatically
     * instead of the whole turn failing outright — which is exactly what
     * happened manually (a retried request succeeded ~1 minute later) before
     * this existed.
     *
     * <p>Not applied to {@link #chatStream}: once an SSE stream has started
     * emitting tokens to the client there's nothing sensible to retry.
     */
    private String callWithRateLimitRetry(Supplier<String> call) {
        for (int attempt = 0; ; attempt++) {
            try {
                return call.get();
            } catch (RuntimeException e) {
                String msg = e.getMessage();
                boolean rateLimited = msg != null && (msg.contains("tokens per minute")
                        || msg.contains(" 429") || msg.contains(" 413"));
                if (!rateLimited || attempt >= RATE_LIMIT_MAX_RETRIES) {
                    throw e;
                }
                long wait = RATE_LIMIT_BACKOFF_MS * (attempt + 1);
                log.warn("Groq rate limit hit, retrying in {}ms (attempt {}/{}): {}",
                        wait, attempt + 1, RATE_LIMIT_MAX_RETRIES, msg);
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }

    // ── Shared pipeline steps ─────────────────────────────────────────────────

    private record PreparedPrompt(Session session, String systemPrompt,
                                   String userPrompt, boolean usedRag) {}

    /** Steps 1-3: load session, run CRAG, fetch memory, assemble prompts. */
    private PreparedPrompt prepare(ChatRequest request) {
        Session session = sessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Session not found: " + request.sessionId()));

        // CRAG: retrieve → grade → context or general-knowledge fallback
        CragService.CragResult crag = cragService.retrieveAndGrade(
                request.sessionId().toString(), request.userId().toString(), request.message());

        // Personalisation from user-memory-service ("prefers Python examples", ...)
        String memoryContext = memoryClient.getMemoryContext(request.userId().toString());

        // Adaptive depth: how well does this student already know what they
        // just asked about? Degrades to "unknown" if knowledge-service is down.
        String depth = depthDirective(
                knowledgeClient.findMasteryForQuestion(request.userId().toString(), request.message()));

        String systemPrompt = String.format(SYSTEM_TEMPLATE,
                request.userId(), request.sessionId(),
                memoryContext, depth, crag.context(), crag.sourceLabel()).strip();

        // Recent history, oldest-first, capped by characters (a rough token
        // guard — 50 long messages could otherwise blow the context window).
        List<Message> recent = messageRepository
                .findTop12BySessionIdOrderByCreatedAtDesc(request.sessionId()).reversed();
        StringBuilder conversation = new StringBuilder();
        for (Message m : recent) {
            String line = m.getRole() + ": " + m.getContent() + "\n";
            if (conversation.length() + line.length() > HISTORY_LIMIT_CHARS) {
                continue; // skip oldest overflow; newest turns matter most
            }
            conversation.append(line);
        }
        conversation.append("user: ").append(request.message());

        return new PreparedPrompt(session, systemPrompt, conversation.toString(), crag.usedRag());
    }

    /** Steps 5-6: save both turns, emit chat.message.saved for each. */
    private Message persistTurns(Session session, ChatRequest request, String reply) {
        Message userMsg = messageRepository.save(
                new Message(session, "user", request.message()));
        Message assistantMsg = messageRepository.save(
                new Message(session, "assistant", reply == null ? "" : reply));

        eventProducer.publishChatMessageSaved(ChatMessageSavedEvent.of(
                userMsg.getId().toString(), session.getId().toString(),
                request.userId().toString(), "user", request.message()));
        eventProducer.publishChatMessageSaved(ChatMessageSavedEvent.of(
                assistantMsg.getId().toString(), session.getId().toString(),
                request.userId().toString(), "assistant", assistantMsg.getContent()));

        return assistantMsg;
    }
}
