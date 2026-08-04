package com.guruai.agent.repository;

import com.guruai.agent.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    // Was findTop50 — fetching 50 messages just to truncate most of them away
    // by HISTORY_LIMIT_CHARS anyway wasted a bigger DB read for no benefit.
    // 12 turns is already generous for conversational continuity.
    List<Message> findTop12BySessionIdOrderByCreatedAtDesc(UUID sessionId);
}
