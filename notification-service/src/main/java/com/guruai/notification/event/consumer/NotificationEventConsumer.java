package com.guruai.notification.event.consumer;

import com.guruai.common.constants.KafkaTopics;
import com.guruai.common.enums.NotificationType;
import com.guruai.common.events.MasteryDroppedEvent;
import com.guruai.common.events.QuizCompletedEvent;
import com.guruai.common.events.UserRegisteredEvent;
import com.guruai.common.events.DocumentIndexedEvent;
import com.guruai.common.events.WeakTopicReminderEvent;
import com.guruai.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = KafkaTopics.USER_REGISTERED, groupId = "notification-service-group")
    public void onUserRegistered(UserRegisteredEvent event) {
        notificationService.sendNotification(
                UUID.fromString(event.userId()),
                event.eventId(),
                NotificationType.WELCOME,
                "Welcome to GuruAI! 🎓",
                "Your personalized AI tutor is ready. Upload a document to get started!"
        );
    }

    @KafkaListener(topics = KafkaTopics.MASTERY_DROPPED, groupId = "notification-service-group")
    public void onMasteryDropped(MasteryDroppedEvent event) {
        String pct = String.format("%.0f%%", event.newEmaScore() * 100);
        notificationService.sendNotification(
                UUID.fromString(event.userId()),
                event.eventId(),
                NotificationType.MASTERY_DROP,
                "Mastery dropped: " + event.topic(),
                String.format("Your mastery in '%s' dropped to %s. Consider reviewing this topic.",
                        event.topic(), pct)
        );
    }

    @KafkaListener(topics = KafkaTopics.QUIZ_COMPLETED, groupId = "notification-service-group")
    public void onQuizCompleted(QuizCompletedEvent event) {
        // This topic now fires once per ANSWERED QUESTION (see QuizCompletedEvent's
        // javadoc) so Knowledge Service can update mastery per-question — only the
        // one event where quizComplete=true represents the whole attempt finishing,
        // which is the only one worth a "quiz completed" notification.
        if (!event.quizComplete()) {
            return;
        }
        String msg = event.totalScore() >= 80
                ? String.format("You scored %d%% on your %s quiz. Excellent work! 🎉",
                        event.totalScore(), event.subject())
                : String.format("You scored %d%% on your %s quiz. Keep practising!",
                        event.totalScore(), event.subject());
        notificationService.sendNotification(
                UUID.fromString(event.userId()),
                event.eventId(),
                NotificationType.QUIZ_STREAK,
                "Quiz completed",
                msg
        );
    }

    @KafkaListener(topics = KafkaTopics.DOCUMENT_INDEXED, groupId = "notification-service-group")
    public void onDocumentIndexed(DocumentIndexedEvent event) {
        notificationService.sendNotification(
                UUID.fromString(event.userId()),
                event.eventId(),
                NotificationType.DOCUMENT_PROCESSED,
                "Document ready: " + event.filename(),
                String.format("'%s' has been indexed (%d chunks). You can now chat and generate quizzes!",
                        event.filename(), event.chunkCount())
        );
    }

    @KafkaListener(topics = KafkaTopics.WEAK_TOPIC_REMINDER, groupId = "notification-service-group")
    public void onWeakTopicReminder(WeakTopicReminderEvent event) {
        notificationService.sendNotification(
                UUID.fromString(event.userId()),
                event.eventId(),
                NotificationType.WEAK_TOPIC_REMINDER,
                "Let's revise: " + event.topic(),
                String.format("Your mastery of '%s' is still weak (%.0f%%). A few minutes now goes a long way — " +
                        "jump back into that session to ask a question or take a quick quiz.",
                        event.topic(), event.emaScore() * 100),
                UUID.fromString(event.sessionId()),
                event.topic()
        );
    }
}
