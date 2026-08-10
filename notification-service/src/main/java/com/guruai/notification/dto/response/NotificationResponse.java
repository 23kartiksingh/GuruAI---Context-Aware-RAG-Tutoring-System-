package com.guruai.notification.dto.response;

import com.guruai.common.enums.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        NotificationType type,
        String title,
        String message,
        boolean isRead,
        // Deep-link target — null for every type except WEAK_TOPIC_REMINDER.
        UUID sessionId,
        String topic,
        Instant createdAt
) {}
