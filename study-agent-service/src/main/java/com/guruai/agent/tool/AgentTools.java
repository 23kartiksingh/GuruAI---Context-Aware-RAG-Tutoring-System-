package com.guruai.agent.tool;

import com.guruai.agent.client.DocumentServiceClient;
import com.guruai.agent.client.KnowledgeServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * The agent's tool belt — methods the LLM can invoke mid-conversation via
 * Spring AI tool calling (registered with {@code .tools(agentTools)} on the
 * ChatClient call in StudyAgentServiceImpl).
 *
 * <p>How it works: Spring AI describes each {@code @Tool} method to the
 * model in the request. If the model decides it needs one (e.g. the student
 * asks "what should I revise?" and it wants real mastery data instead of
 * guessing), it replies with a tool-call request; Spring AI executes the
 * method here and sends the result back to the model, which then writes its
 * final answer. Zero orchestration code on our side — this is the "ReAct
 * agent" part of study-agent-service.
 *
 * <p>The {@code userId}/{@code sessionId} parameters are filled in BY THE
 * MODEL from values we put in the system prompt each turn ("Current student
 * userId: ..."), since tool methods are stateless and know nothing about
 * the current request otherwise.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentTools {

    private final KnowledgeServiceClient knowledgeClient;
    private final DocumentServiceClient documentClient;

    // Returned instead of doing real work on a repeat call — see ToolCallGuard.
    // Deliberately short: the whole point is to stop feeding the model a large
    // payload it's already been given once this turn.
    private static final String ALREADY_CALLED =
            "You already called this tool with these exact arguments earlier in this turn — "
            + "re-use that result instead of calling it again. Answer the student's question now.";

    @Tool(description = """
            Get the student's mastery profile (topic, %, WEAK/AVERAGE/STRONG).
            Use for "what should I study/how am I doing" questions. Call this AT
            MOST ONCE per turn — calling it again returns the same data.""")
    public String getStudentMasteryProfile(
            @ToolParam(description = "The student's userId (given in the system prompt)") String userId) {
        if (!ToolCallGuard.firstTime("getStudentMasteryProfile", userId)) {
            log.warn("Tool call BLOCKED (duplicate): getStudentMasteryProfile(userId={})", userId);
            return ALREADY_CALLED;
        }
        log.info("Tool call: getStudentMasteryProfile(userId={})", userId);
        return knowledgeClient.getMasteryProfileSummary(userId);
    }

    @Tool(description = """
            Get only the student's WEAK topics, for building a revision plan. Call
            this AT MOST ONCE per turn — calling it again returns the same data.""")
    public String getStudentWeakTopics(
            @ToolParam(description = "The student's userId (given in the system prompt)") String userId) {
        if (!ToolCallGuard.firstTime("getStudentWeakTopics", userId)) {
            log.warn("Tool call BLOCKED (duplicate): getStudentWeakTopics(userId={})", userId);
            return ALREADY_CALLED;
        }
        log.info("Tool call: getStudentWeakTopics(userId={})", userId);
        var weak = knowledgeClient.getWeakTopics(userId);
        return weak.isEmpty()
                ? "No weak topics recorded — the student either hasn't been assessed yet or is doing well everywhere."
                : String.join("\n", weak);
    }

    @Tool(description = """
            Search the student's uploaded documents for a query. Only use this if
            the context you already have doesn't cover what they're asking. Don't
            repeat the same query — if it didn't find anything the first time, it
            won't the second.""")
    public String searchStudentDocuments(
            @ToolParam(description = "The current sessionId (given in the system prompt)") String sessionId,
            @ToolParam(description = "The student's userId (given in the system prompt)") String userId,
            @ToolParam(description = "What to search for") String query) {
        if (!ToolCallGuard.firstTime("searchStudentDocuments", sessionId, query)) {
            log.warn("Tool call BLOCKED (duplicate): searchStudentDocuments(sessionId={}, query={})",
                     sessionId, query);
            return ALREADY_CALLED;
        }
        log.info("Tool call: searchStudentDocuments(sessionId={}, query={})", sessionId, query);
        var chunks = documentClient.searchChunks(sessionId, userId, query, 3);
        return chunks.isEmpty()
                ? "No matching passages found in the uploaded documents."
                : String.join("\n---\n", chunks);
    }
}
