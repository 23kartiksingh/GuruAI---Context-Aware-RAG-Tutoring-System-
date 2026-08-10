package com.guruai.knowledge.controller;

import com.guruai.common.dto.ApiResponse;
import com.guruai.knowledge.dto.request.AddSubjectRequest;
import com.guruai.knowledge.dto.response.MasteryProfileResponse;
import com.guruai.knowledge.dto.response.TopicMasteryResponse;
import com.guruai.knowledge.dto.response.UserStatsResponse;
import com.guruai.knowledge.service.MasteryService;
import com.guruai.knowledge.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final MasteryService masteryService;
    private final SubjectService subjectService;

    /** Full mastery profile — all topics, counts, overall percentage. */
    @GetMapping("/{userId}/profile")
    public ResponseEntity<ApiResponse<MasteryProfileResponse>> getProfile(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(masteryService.getMasteryProfile(userId)));
    }

    /** All weak topics for a user (EMA < 0.4). */
    @GetMapping("/{userId}/weak-topics")
    public ResponseEntity<ApiResponse<List<TopicMasteryResponse>>> getWeakTopics(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(masteryService.getWeakTopics(userId)));
    }

    /** All strong topics for a user (EMA >= 0.7). */
    @GetMapping("/{userId}/strong-topics")
    public ResponseEntity<ApiResponse<List<TopicMasteryResponse>>> getStrongTopics(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(masteryService.getStrongTopics(userId)));
    }

    /** Aggregate stats — total questions, correct answers, avg mastery %. */
    @GetMapping("/{userId}/stats")
    public ResponseEntity<ApiResponse<UserStatsResponse>> getStats(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(masteryService.getUserStats(userId)));
    }

    /** All subjects the user is enrolled in. */
    @GetMapping("/{userId}/subjects")
    public ResponseEntity<ApiResponse<List<String>>> getSubjects(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(subjectService.getSubjects(userId)));
    }

    /** Enrol the user in a new subject. */
    @PostMapping("/{userId}/subjects")
    public ResponseEntity<ApiResponse<String>> addSubject(
            @PathVariable UUID userId,
            @Valid @RequestBody AddSubjectRequest request) {
        // Manual enrolment isn't tied to a session, so it's stored with a null
        // sessionId and survives session deletion.
        subjectService.addSubject(userId, null, request.subject());
        return ResponseEntity.ok(ApiResponse.ok("Subject added: " + request.subject()));
    }

    /** Remove a subject from the user's enrolled list. */
    @DeleteMapping("/{userId}/subjects/{subject}")
    public ResponseEntity<ApiResponse<String>> removeSubject(
            @PathVariable UUID userId,
            @PathVariable String subject) {
        subjectService.removeSubject(userId, subject);
        return ResponseEntity.ok(ApiResponse.ok("Subject removed: " + subject));
    }

    /** Mastery for a specific topic. */
    @GetMapping("/{userId}/topics")
    public ResponseEntity<ApiResponse<List<TopicMasteryResponse>>> getTopics(
            @PathVariable UUID userId,
            @RequestParam(required = false) String subject) {
        MasteryProfileResponse profile = masteryService.getMasteryProfile(userId);
        List<TopicMasteryResponse> topics = subject == null ? profile.topics() :
                profile.topics().stream()
                        .filter(t -> t.subject().equalsIgnoreCase(subject))
                        .toList();
        return ResponseEntity.ok(ApiResponse.ok(topics));
    }
}
