package com.guruai.common.enums;

/**
 * Represents a student's mastery level for a specific topic, based on their
 * Exponential Moving Average (EMA) score.
 *
 * <p>EMA thresholds (α = 0.3, ported from original Python {@code tracker.py}):
 * <ul>
 *   <li>{@code WEAK}    — EMA score &lt; 0.50  (less than 50%)</li>
 *   <li>{@code AVERAGE} — EMA score 0.50 – 0.75</li>
 *   <li>{@code STRONG}  — EMA score &gt; 0.75</li>
 * </ul>
 */
public enum MasteryLevel {

    /** EMA score < 0.50 — needs significant work. */
    WEAK(0.0, 0.5),

    /** EMA score 0.50–0.75 — developing, some gaps remain. */
    AVERAGE(0.5, 0.75),

    /** EMA score > 0.75 — well understood. */
    STRONG(0.75, 1.0);

    private final double minScore;
    private final double maxScore;

    MasteryLevel(double minScore, double maxScore) {
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

    public double getMinScore() { return minScore; }
    public double getMaxScore() { return maxScore; }

    /**
     * Classify an EMA score into the appropriate mastery level.
     *
     * @param emaScore EMA score in range [0.0, 1.0]
     * @return the corresponding {@link MasteryLevel}
     */
    public static MasteryLevel fromScore(double emaScore) {
        if (emaScore > 0.75) return STRONG;
        if (emaScore >= 0.5) return AVERAGE;
        return WEAK;
    }

    /** @return a human-readable percentage string (e.g. "72%") */
    public String toPercentString(double emaScore) {
        return Math.round(emaScore * 100) + "%";
    }
}
