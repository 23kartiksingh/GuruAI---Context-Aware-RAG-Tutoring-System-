package com.guruai.notification.entity;

import com.guruai.common.enums.NotificationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * The originating Kafka event's ID — the idempotency key.
     *
     * <p>Unique (where non-null), so a redelivered or replayed event can't
     * create a second identical notification. Null for rows created before
     * this column existed.
     */
    @Column(name = "event_id", length = 64)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    /** Deep-link target — which session's chat this notification is about,
     *  if any. Null for every type except WEAK_TOPIC_REMINDER today. */
    @Column(name = "session_id")
    private UUID sessionId;

    /** The topic this notification is about, if any — lets the frontend
     *  pre-fill "revise this" flows (quiz topic, chat prompt) on click. */
    @Column(name = "topic", length = 200)
    private String topic;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public Notification(UUID userId, String eventId, NotificationType type,
                        String title, String message) {
        this.userId = userId;
        this.eventId = eventId;
        this.type = type;
        this.title = title;
        this.message = message;
    }

    public Notification(UUID userId, String eventId, NotificationType type,
                        String title, String message, UUID sessionId, String topic) {
        this(userId, eventId, type, title, message);
        this.sessionId = sessionId;
        this.topic = topic;
    }
}
