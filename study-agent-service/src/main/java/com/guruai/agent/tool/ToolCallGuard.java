package com.guruai.agent.tool;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-turn guard against a model repeatedly re-calling the exact same tool
 * with the exact same arguments instead of reusing the answer it already
 * has. Observed in production: llama-3.1-8b-instant called
 * {@code getStudentMasteryProfile} with the same userId 50+ times within a
 * few hundred milliseconds in a single turn — and because every tool
 * round-trip resends the whole growing conversation to Groq, that alone blew
 * through the entire 6000 TPM free-tier budget in one turn (413/429 errors,
 * multi-minute stalls waiting out the rate-limit retry backoff).
 *
 * <p>Scoped via {@link ThreadLocal} rather than request-scoped Spring
 * machinery because the blocking {@code chat()} path keeps the ENTIRE
 * tool-calling loop — every Groq round-trip and every tool execution — on
 * one request-handling thread; see {@code StudyAgentServiceImpl#chat}, which
 * calls {@link #reset()} before the call and {@link #clear()} in a
 * {@code finally} block so a pooled thread never leaks state into an
 * unrelated later request.
 */
public final class ToolCallGuard {

    private static final ThreadLocal<Set<String>> SEEN =
            ThreadLocal.withInitial(ConcurrentHashMap::newKeySet);

    private ToolCallGuard() {}

    /** Call at the start of every chat turn, before the model sees any tools. */
    public static void reset() {
        SEEN.get().clear();
    }

    /** Call in a finally block at the end of every chat turn — avoids leaking into a reused pooled thread. */
    public static void clear() {
        SEEN.remove();
    }

    /**
     * @return {@code true} the first time this exact (tool, args) signature is
     *         seen this turn, {@code false} on every repeat — callers should
     *         short-circuit on {@code false} instead of doing real work.
     */
    public static boolean firstTime(String toolName, Object... args) {
        return SEEN.get().add(toolName + "|" + Arrays.toString(args));
    }
}
