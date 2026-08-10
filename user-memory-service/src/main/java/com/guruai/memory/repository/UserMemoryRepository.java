package com.guruai.memory.repository;

import com.guruai.memory.entity.UserMemory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserMemoryRepository extends JpaRepository<UserMemory, UUID> {

    List<UserMemory> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<UserMemory> findByUserIdAndItemHash(UUID userId, String itemHash);

    boolean existsByUserIdAndItemHash(UUID userId, String itemHash);

    void deleteByUserId(UUID userId);

    /** Ownership-scoped lookup — a user can only edit/delete their own items. */
    Optional<UserMemory> findByIdAndUserId(UUID id, UUID userId);
}
