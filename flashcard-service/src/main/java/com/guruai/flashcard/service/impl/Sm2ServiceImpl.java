package com.guruai.flashcard.service.impl;

import com.guruai.flashcard.entity.Flashcard;
import com.guruai.flashcard.service.Sm2Service;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class Sm2ServiceImpl implements Sm2Service {

    private static final double MIN_EASE_FACTOR = 1.3;

    /**
     * SM-2 algorithm:
     * <pre>
     *   EF' = EF + (0.1 - (5-q) * (0.08 + (5-q) * 0.02))
     *   EF  >= 1.3 always
     *   If q < 3: reset repetitions to 0, interval to 1
     *   Else:
     *     rep == 0 → interval = 1
     *     rep == 1 → interval = 6
     *     else     → interval = round(prev_interval * EF)
     * </pre>
     */
    @Override
    public void applyReview(Flashcard card, int quality) {
        // Clamp quality to 0-5
        int q = Math.max(0, Math.min(5, quality));

        // Update ease factor
        double ef = card.getEaseFactor();
        ef = ef + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02));
        ef = Math.max(MIN_EASE_FACTOR, ef);
        card.setEaseFactor(ef);

        if (q < 3) {
            // Failed recall — reset to beginning
            card.setRepetitions(0);
            card.setIntervalDays(1);
        } else {
            // Successful recall — advance interval
            int rep = card.getRepetitions();
            int newInterval;
            if (rep == 0) {
                newInterval = 1;
            } else if (rep == 1) {
                newInterval = 6;
            } else {
                newInterval = (int) Math.ceil(card.getIntervalDays() * ef);
            }
            card.setRepetitions(rep + 1);
            card.setIntervalDays(newInterval);
        }

        card.setNextReviewDate(LocalDate.now().plusDays(card.getIntervalDays()));
    }
}
