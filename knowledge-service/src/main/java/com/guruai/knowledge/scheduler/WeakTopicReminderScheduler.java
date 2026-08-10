package com.guruai.knowledge.scheduler;

import com.guruai.common.events.WeakTopicReminderEvent;
import com.guruai.knowledge.dto.response.TopicMasteryResponse;
import com.guruai.knowledge.event.producer.KnowledgeEventProducer;
import com.guruai.knowledge.repository.TopicMasteryRepository;
import com.guruai.knowledge.service.MasteryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Periodic "let's revise this" nudge for students with a weak topic —
 * proactive, unlike {@code mastery.dropped} which only fires the instant a
 * topic regresses.
 *
 * <p>Runs every {@code guruai.mastery.reminder-interval-ms} (default 10
 * minutes), but that's how often the CHECK runs, not how often a given
 * student gets notified — a Redis cooldown key limits any one student to at
 * most one reminder per {@code guruai.mastery.reminder-cooldown-hours}
 * (default 4h), otherwise the same weak topic would re-notify every single
 * check for as long as it stayed weak.
 *
 * <p>Picks the single WEAKEST topic per student (not one notification per
 * weak topic) to avoid spamming someone with five topics needing work, and
 * only considers topics with a live {@code sessionId} — a topic from a
 * session that's since been deleted has nowhere to deep-link to.
 *
 * <p>No real online/presence tracking exists in this app, so "online" from
 * the product ask is approximated as "gets a reminder when the periodic
 * check runs and isn't in cooldown" rather than true presence detection —
 * flagged here since that's a simplification, not the literal ask.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeakTopicReminderScheduler {

    private static final String COOLDOWN_KEY_PREFIX = "weak-reminder-cooldown:";

    private final TopicMasteryRepository masteryRepository;
    private final MasteryService masteryService;
    private final KnowledgeEventProducer eventProducer;
    private final StringRedisTemplate redisTemplate;

    @Value("${guruai.mastery.reminder-cooldown-hours:4}")
    private long cooldownHours;

    @Scheduled(fixedRateString = "${guruai.mastery.reminder-interval-ms:600000}")
    public void checkWeakTopics() {
        List<UUID> userIds = masteryRepository.findDistinctUserIdsWithWeakTopic();
        if (userIds.isEmpty()) {
            return;
        }
        log.debug("Weak-topic reminder check: {} student(s) currently have a weak topic", userIds.size());

        int sent = 0;
        for (UUID userId : userIds) {
            if (isInCooldown(userId)) {
                continue;
            }
            Optional<TopicMasteryResponse> weakest = pickWeakestWithSession(userId);
            if (weakest.isEmpty()) {
                continue; // has weak topics, but none tied to a live session
            }
            TopicMasteryResponse topic = weakest.get();
            eventProducer.publishWeakTopicReminder(WeakTopicReminderEvent.of(
                    userId.toString(), topic.sessionId().toString(),
                    topic.subject(), topic.topic(), topic.emaScore()));
            startCooldown(userId);
            sent++;
        }
        if (sent > 0) {
            log.info("Weak-topic reminder check: sent {} reminder(s)", sent);
        }
    }

    private Optional<TopicMasteryResponse> pickWeakestWithSession(UUID userId) {
        return masteryService.getWeakTopics(userId).stream()
                .filter(t -> t.sessionId() != null)
                .min(Comparator.comparingDouble(TopicMasteryResponse::emaScore));
    }

    private boolean isInCooldown(UUID userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(COOLDOWN_KEY_PREFIX + userId));
    }

    private void startCooldown(UUID userId) {
        redisTemplate.opsForValue().set(
                COOLDOWN_KEY_PREFIX + userId, "1", Duration.ofHours(cooldownHours));
    }
}
