package com.guruai.agent.repository;

import com.guruai.agent.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    List<Session> findByUserIdOrderByUpdatedAtDesc(UUID userId);
}
