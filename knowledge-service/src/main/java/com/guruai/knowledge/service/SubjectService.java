package com.guruai.knowledge.service;

import java.util.List;
import java.util.UUID;

public interface SubjectService {

    List<String> getSubjects(UUID userId);

    /**
     * Enrol the user in a subject, scoped to the session it came from.
     *
     * @param sessionId may be null for a manual enrolment not tied to a session
     */
    void addSubject(UUID userId, UUID sessionId, String subject);

    void removeSubject(UUID userId, String subject);

    /** Remove subject enrolments created by a session (session was deleted). */
    int deleteBySession(UUID userId, UUID sessionId);

    void initializeUserProfile(UUID userId);
}
