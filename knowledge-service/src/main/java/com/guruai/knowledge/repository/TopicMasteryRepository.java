package com.guruai.knowledge.repository;

import com.guruai.common.enums.MasteryLevel;
import com.guruai.knowledge.entity.TopicMastery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TopicMasteryRepository extends JpaRepository<TopicMastery, UUID> {

    List<TopicMastery> findByUserId(UUID userId);

    List<TopicMastery> findByUserIdAndSubject(UUID userId, String subject);

    Optional<TopicMastery> findByUserIdAndSubjectAndTopic(UUID userId, String subject, String topic);

    // ── Session-scoped (mastery is tracked per study session) ────────────────

    List<TopicMastery> findByUserIdAndSessionIdAndSubject(UUID userId, UUID sessionId, String subject);

    Optional<TopicMastery> findByUserIdAndSessionIdAndSubjectAndTopic(
            UUID userId, UUID sessionId, String subject, String topic);

    /** Cleanup when a session is deleted. Scoped by user so a bad event can't
     *  reach another user's rows. */
    @Modifying
    @Query("DELETE FROM TopicMastery tm WHERE tm.userId = :userId AND tm.sessionId = :sessionId")
    int deleteByUserIdAndSessionId(UUID userId, UUID sessionId);

    List<TopicMastery> findByUserIdAndMasteryLevel(UUID userId, MasteryLevel masteryLevel);

    /** Every student who currently has at least one WEAK topic — the
     *  candidate pool for WeakTopicReminderScheduler's periodic check. */
    @Query("SELECT DISTINCT tm.userId FROM TopicMastery tm WHERE tm.masteryLevel = 'WEAK'")
    List<UUID> findDistinctUserIdsWithWeakTopic();

    @Query("SELECT COALESCE(SUM(tm.totalCount), 0) FROM TopicMastery tm WHERE tm.userId = :userId")
    int sumTotalCountByUserId(UUID userId);

    @Query("SELECT COALESCE(SUM(tm.correctCount), 0) FROM TopicMastery tm WHERE tm.userId = :userId")
    int sumCorrectCountByUserId(UUID userId);

    @Query("SELECT COALESCE(AVG(tm.emaScore), 0.5) FROM TopicMastery tm WHERE tm.userId = :userId")
    double avgEmaScoreByUserId(UUID userId);
}
