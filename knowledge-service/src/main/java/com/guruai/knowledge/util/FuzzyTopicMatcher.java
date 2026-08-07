package com.guruai.knowledge.util;

import java.util.List;
import java.util.Optional;

/**
 * Fuzzy topic name matching using Levenshtein distance.
 * Ported from Python's difflib usage in tracker.py.
 * Prevents duplicate topics like "Neural Networks" and "neural network" from
 * being stored as separate entries.
 */
public final class FuzzyTopicMatcher {

    private static final int DEFAULT_THRESHOLD = 3;

    private FuzzyTopicMatcher() {}

    /** Lowercase, trim, collapse whitespace. */
    public static String normalize(String topic) {
        if (topic == null) return "";
        return topic.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    /** Levenshtein distance between two strings (pure Java). */
    public static int levenshteinDistance(String a, String b) {
        int m = a.length(), n = b.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1],
                               Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }
        return dp[m][n];
    }

    public static boolean isSimilar(String a, String b, int threshold) {
        return levenshteinDistance(normalize(a), normalize(b)) <= threshold;
    }

    /**
     * Finds the best matching topic from existing topics, or returns the
     * original topic if no close match is found.
     *
     * @param topic          incoming topic name
     * @param existingTopics list of already-stored topic names
     * @return the best matching existing topic, or {@code topic} itself
     */
    public static String findBestMatch(String topic, List<String> existingTopics) {
        if (existingTopics == null || existingTopics.isEmpty()) return topic;
        String normalizedInput = normalize(topic);
        String bestMatch = null;
        int bestDist = Integer.MAX_VALUE;
        for (String existing : existingTopics) {
            int dist = levenshteinDistance(normalizedInput, normalize(existing));
            if (dist < bestDist) {
                bestDist = dist;
                bestMatch = existing;
            }
        }
        return (bestDist <= DEFAULT_THRESHOLD && bestMatch != null) ? bestMatch : topic;
    }

    /**
     * Same matching as {@link #findBestMatch}, but returns empty instead of
     * falling back to the input topic when nothing close exists — so the
     * caller can tell "matched an existing topic" apart from "genuinely
     * nothing close" (e.g. to decide whether a slower semantic
     * canonicalization step is worth running).
     */
    public static Optional<String> findClosestMatch(String topic, List<String> existingTopics) {
        if (existingTopics == null || existingTopics.isEmpty()) return Optional.empty();
        String normalizedInput = normalize(topic);
        String bestMatch = null;
        int bestDist = Integer.MAX_VALUE;
        for (String existing : existingTopics) {
            int dist = levenshteinDistance(normalizedInput, normalize(existing));
            if (dist < bestDist) {
                bestDist = dist;
                bestMatch = existing;
            }
        }
        return (bestDist <= DEFAULT_THRESHOLD && bestMatch != null)
                ? Optional.of(bestMatch) : Optional.empty();
    }
}
