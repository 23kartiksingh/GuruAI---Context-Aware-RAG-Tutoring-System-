package com.guruai.knowledge.service.impl;

import com.guruai.knowledge.entity.UserSubject;
import com.guruai.knowledge.repository.UserSubjectRepository;
import com.guruai.knowledge.service.SubjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final UserSubjectRepository subjectRepository;

    @Override
    @Transactional(readOnly = true)
    public List<String> getSubjects(UUID userId) {
        // Enrolments are per-session now, so the same subject can appear once
        // per session — the caller wants the distinct set.
        return subjectRepository.findByUserId(userId)
                .stream()
                .map(UserSubject::getSubject)
                .distinct()
                .toList();
    }

    @Override
    @Transactional
    public void addSubject(UUID userId, UUID sessionId, String subject) {
        if (!subjectRepository.existsByUserIdAndSessionIdAndSubject(userId, sessionId, subject)) {
            subjectRepository.save(new UserSubject(userId, sessionId, subject));
            log.info("Added subject '{}' for userId={} sessionId={}", subject, userId, sessionId);
        }
    }

    @Override
    @Transactional
    public int deleteBySession(UUID userId, UUID sessionId) {
        int removed = subjectRepository.deleteByUserIdAndSessionId(userId, sessionId);
        log.info("Deleted {} subject enrolment(s) for userId={} sessionId={}", removed, userId, sessionId);
        return removed;
    }

    @Override
    @Transactional
    public void removeSubject(UUID userId, String subject) {
        subjectRepository.deleteByUserIdAndSubject(userId, subject);
        log.info("Removed subject '{}' for userId={}", subject, userId);
    }

    @Override
    @Transactional
    public void initializeUserProfile(UUID userId) {
        // On registration, we just log — subjects are added by the user later
        log.info("Initialized knowledge profile for new userId={}", userId);
    }
}
