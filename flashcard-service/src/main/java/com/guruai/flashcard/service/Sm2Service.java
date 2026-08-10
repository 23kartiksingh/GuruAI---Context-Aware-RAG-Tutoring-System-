package com.guruai.flashcard.service;

import com.guruai.flashcard.entity.Flashcard;

/**
 * Implements the SM-2 spaced repetition algorithm.
 * Ported from SuperMemo algorithm specification.
 */
public interface Sm2Service {

    /**
     * Applies one SM-2 review step to a flashcard in-place.
     *
     * @param card    the flashcard to update
     * @param quality SM-2 quality score (0–5)
     */
    void applyReview(Flashcard card, int quality);
}
