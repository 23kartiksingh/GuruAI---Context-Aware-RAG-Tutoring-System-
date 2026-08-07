package com.guruai.knowledge.service.impl;

import com.guruai.common.enums.MasteryLevel;
import com.guruai.common.events.MasteryDroppedEvent;
import com.guruai.knowledge.config.MasteryProperties;
import com.guruai.knowledge.dto.response.MasteryProfileResponse;
import com.guruai.knowledge.dto.response.TopicMasteryResponse;
import com.guruai.knowledge.dto.response.UserStatsResponse;
import com.guruai.knowledge.entity.TopicMastery;
import com.guruai.knowledge.event.producer.KnowledgeEventProducer;
import com.guruai.knowledge.mapper.MasteryMapper;
import com.guruai.knowledge.repository.TopicMasteryRepository;
import com.guruai.knowledge.service.MasteryService;
import com.guruai.knowledge.util.FuzzyTopicMatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MasteryServiceImpl implements MasteryService {

    private final TopicMasteryRepository masteryRepository;
    private final MasteryMapper masteryMapper;
    private final MasteryProperties masteryProperties;
    private final KnowledgeEventProducer eventProducer;
    private final TopicCanonicalizerService topicCanonicalizer;

    @Override
    @Transactional(readOnly = true)
    public MasteryProfileResponse getMasteryProfile(UUID userId) {
        List<TopicMastery> topics = masteryRepository.findByUserId(userId);
        return masteryMapper.toProfileResponse(userId, topics);
    }

    @Override
    @Transactional(readOnly = true)
    public TopicMasteryResponse getTopicMastery(UUID userId, String subject, String topic) {
        return masteryRepository
                .findByUserIdAndSubjectAndTopic(userId, subject, topic)
                .map(masteryMapper::toResponse)
                .orElseGet(() -> {
                    // Return a default neutral response if no record exists yet
                    TopicMastery neutral = new TopicMastery(userId, null, subject, topic);
                    neutral.setMasteryLevel(MasteryLevel.AVERAGE);
                    return masteryMapper.toResponse(neutral);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public UserStatsResponse getUserStats(UUID userId) {
        int total = masteryRepository.sumTotalCountByUserId(userId);
        int correct = masteryRepository.sumCorrectCountByUserId(userId);
        double avgMastery = masteryRepository.avgEmaScoreByUserId(userId) * 100.0;
        return new UserStatsResponse(userId, total, correct,
                Math.round(avgMastery * 10.0) / 10.0);
    }

    @Override
    @Transactional
    public void updateMastery(UUID userId, UUID sessionId, String subject, String topic, boolean correct) {
        // Cumulate similar topics under ONE name instead of letting every
        // slightly different phrasing become its own record — looked up
        // ACROSS ALL of the user's sessions for this subject, not just the
        // current one, since the goal is one canonical "DBMS" per student,
        // not one per session. The mastery ROW itself stays scoped by
        // sessionId below (needed for the session-delete cascade); only the
        // topic NAME benefits from this wider view.
        List<String> existingTopics = masteryRepository
                .findByUserIdAndSubject(userId, subject)
                .stream()
                .map(TopicMastery::getTopic)
                .distinct()
                .toList();

        // Cheap string-similarity pass first (typos, casing, singular vs
        // plural). Only when that finds nothing close do we spend a Groq
        // call on semantic classification — e.g. "how do I join two
        // tables?" doesn't textually resemble "DBMS" at all, but should
        // still land under it.
        String resolvedTopic = FuzzyTopicMatcher.findClosestMatch(topic, existingTopics)
                .orElseGet(() -> {
                    String canonical = topicCanonicalizer.canonicalize(topic, subject, existingTopics);
                    // The model's answer might itself be a near-miss of an
                    // existing name (e.g. slightly different casing) —
                    // catch that before creating yet another near-duplicate.
                    return FuzzyTopicMatcher.findClosestMatch(canonical, existingTopics).orElse(canonical);
                });

        TopicMastery mastery = masteryRepository
                .findByUserIdAndSessionIdAndSubjectAndTopic(userId, sessionId, subject, resolvedTopic)
                .orElseGet(() -> new TopicMastery(userId, sessionId, subject, resolvedTopic));

        MasteryLevel previousLevel = mastery.getMasteryLevel();

        mastery.applyEmaUpdate(correct,
                masteryProperties.emaAlpha(),
                masteryProperties.weakThreshold(),
                masteryProperties.strongThreshold());

        masteryRepository.save(mastery);

        // Publish mastery.dropped if regressed to WEAK
        if (mastery.getMasteryLevel() == MasteryLevel.WEAK
                && previousLevel != MasteryLevel.WEAK
                && mastery.getTotalCount() > 1) {
            log.info("Mastery dropped for userId={} subject={} topic={} score={}",
                    userId, subject, resolvedTopic, mastery.getEmaScore());
            eventProducer.publishMasteryDropped(MasteryDroppedEvent.of(
                    userId.toString(), subject, resolvedTopic, previousLevel, mastery.getEmaScore()));
        }
    }

    @Override
    @Transactional
    public int deleteBySession(UUID userId, UUID sessionId) {
        int removed = masteryRepository.deleteByUserIdAndSessionId(userId, sessionId);
        log.info("Deleted {} mastery record(s) for userId={} sessionId={}", removed, userId, sessionId);
        return removed;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopicMasteryResponse> getWeakTopics(UUID userId) {
        return masteryRepository.findByUserIdAndMasteryLevel(userId, MasteryLevel.WEAK)
                .stream().map(masteryMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopicMasteryResponse> getStrongTopics(UUID userId) {
        return masteryRepository.findByUserIdAndMasteryLevel(userId, MasteryLevel.STRONG)
                .stream().map(masteryMapper::toResponse).toList();
    }
}
