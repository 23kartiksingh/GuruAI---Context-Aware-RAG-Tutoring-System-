package com.guruai.notification.service;

import com.guruai.common.enums.NotificationType;
import com.guruai.notification.dto.response.NotificationResponse;
import com.guruai.notification.entity.Notification;
import com.guruai.notification.mapper.NotificationMapper;
import com.guruai.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Create and push a notification, ignoring events already processed.
     *
     * <p>Kafka delivers at-least-once, so the same event can legitimately
     * arrive more than once (rebalance, offset reset, redelivery after a
     * failed commit). {@code eventId} is the idempotency key: if a
     * notification already exists for it, this is a replay and we skip it
     * rather than creating a duplicate row and re-pushing over WebSocket.
     *
     * <p>The pre-check handles the ordinary case; the unique index on
     * {@code event_id} is the real guarantee, and catching its violation
     * covers two consumer threads racing on the same event.
     */
    @Transactional
    public void sendNotification(UUID userId, String eventId, NotificationType type,
                                  String title, String message) {
        sendNotification(userId, eventId, type, title, message, null, null);
    }

    /** Same as the 5-arg overload, but for a notification that deep-links to
     *  a session (and optionally a topic) — currently only used for
     *  WEAK_TOPIC_REMINDER. */
    @Transactional
    public void sendNotification(UUID userId, String eventId, NotificationType type,
                                  String title, String message, UUID sessionId, String topic) {
        if (eventId != null && notificationRepository.existsByEventId(eventId)) {
            log.debug("Skipping already-processed event {} ({})", eventId, type);
            return;
        }

        Notification notification = new Notification(userId, eventId, type, title, message, sessionId, topic);
        try {
            notificationRepository.saveAndFlush(notification);
        } catch (DataIntegrityViolationException ex) {
            log.debug("Concurrent duplicate for event {} ({}) — skipping", eventId, type);
            return;
        }

        NotificationResponse response = notificationMapper.toResponse(notification);

        // Push to WebSocket topic for this user
        messagingTemplate.convertAndSendToUser(
                userId.toString(), "/queue/notifications", response);

        log.info("Sent {} notification to userId={}: {}", type, userId, title);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(notificationMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnread(UUID userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream().map(notificationMapper::toResponse).toList();
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllReadForUser(userId);
    }
}
