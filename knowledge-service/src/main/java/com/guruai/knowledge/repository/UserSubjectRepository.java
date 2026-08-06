package com.guruai.knowledge.repository;

import com.guruai.knowledge.entity.UserSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSubjectRepository extends JpaRepository<UserSubject, UUID> {

    List<UserSubject> findByUserId(UUID userId);

    Optional<UserSubject> findByUserIdAndSubject(UUID userId, String subject);

    boolean existsByUserIdAndSubject(UUID userId, String subject);

    void deleteByUserIdAndSubject(UUID userId, String subject);

    // ── Session-scoped ───────────────────────────────────────────────────────

    boolean existsByUserIdAndSessionIdAndSubject(UUID userId, UUID sessionId, String subject);

    /** Cleanup when a session is deleted. Scoped by user for safety. */
    @Modifying
    @Query("DELETE FROM UserSubject us WHERE us.userId = :userId AND us.sessionId = :sessionId")
    int deleteByUserIdAndSessionId(UUID userId, UUID sessionId);
}
