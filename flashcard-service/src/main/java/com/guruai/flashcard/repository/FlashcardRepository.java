package com.guruai.flashcard.repository;

import com.guruai.flashcard.entity.Flashcard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FlashcardRepository extends JpaRepository<Flashcard, UUID> {

    /** Returns all cards due for review on or before the given date. */
    List<Flashcard> findByUserIdAndNextReviewDateLessThanEqual(UUID userId, LocalDate dueDate);

    List<Flashcard> findByUserId(UUID userId);

    List<Flashcard> findByUserIdAndSubject(UUID userId, String subject);

    List<Flashcard> findBySessionId(UUID sessionId);

    /** Cleanup when a session is deleted. Scoped by user so a malformed
     *  event can't reach another user's cards. */
    List<Flashcard> findByUserIdAndSessionId(UUID userId, UUID sessionId);

    long countByUserIdAndNextReviewDateLessThanEqual(UUID userId, LocalDate dueDate);
}
