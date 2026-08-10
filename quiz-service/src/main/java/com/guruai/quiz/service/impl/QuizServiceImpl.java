package com.guruai.quiz.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guruai.common.enums.DifficultyLevel;
import com.guruai.quiz.client.KnowledgeServiceClient;
import com.guruai.common.events.QuizCompletedEvent;
import com.guruai.common.exception.ResourceNotFoundException;
import com.guruai.quiz.dto.request.GenerateQuizRequest;
import com.guruai.quiz.dto.request.SubmitAnswerRequest;
import com.guruai.quiz.dto.response.AnswerResultResponse;
import com.guruai.quiz.dto.response.QuizQuestionResponse;
import com.guruai.quiz.dto.response.QuizResponse;
import com.guruai.quiz.entity.Quiz;
import com.guruai.quiz.entity.QuestionRef;
import com.guruai.quiz.event.producer.QuizEventProducer;
import com.guruai.quiz.repository.QuestionRefRepository;
import com.guruai.quiz.repository.QuizRepository;
import com.guruai.quiz.service.QuizGeneratorService;
import com.guruai.quiz.service.QuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRefRepository questionRefRepository;
    private final QuizGeneratorService quizGeneratorService;
    private final KnowledgeServiceClient knowledgeClient;
    private final QuizEventProducer eventProducer;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public QuizResponse generateQuiz(GenerateQuizRequest request) {
        // A null difficulty means the user picked "Auto" — derive it from how
        // well they currently know this subject/topic, so the quiz meets them
        // where they are instead of making them self-assess.
        DifficultyLevel difficulty = request.difficulty() != null
                ? request.difficulty()
                : knowledgeClient.resolveDifficulty(
                        request.userId().toString(), request.subject(), request.topic());

        // Generate against the resolved difficulty, not the (possibly null) requested one.
        GenerateQuizRequest resolved = new GenerateQuizRequest(
                request.userId(), request.sessionId(), request.subject(),
                request.topic(), difficulty, request.questionCount());

        List<QuestionRef> generatedRefs = quizGeneratorService.generate(resolved);

        Quiz quiz = new Quiz(
                request.userId(), request.sessionId(),
                request.subject(), request.topic(),
                difficulty, generatedRefs.size()
        );
        quiz = quizRepository.save(quiz);

        for (QuestionRef ref : generatedRefs) {
            ref.setQuiz(quiz);
        }
        questionRefRepository.saveAll(generatedRefs);

        return toResponse(quiz, generatedRefs);
    }

    @Override
    @Transactional
    public AnswerResultResponse submitAnswer(UUID questionId, SubmitAnswerRequest request) {
        QuestionRef ref = questionRefRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));

        boolean correct = ref.getCorrectAnswer().equalsIgnoreCase(request.answer());
        ref.setUserAnswer(request.answer());
        ref.setIsCorrect(correct);
        questionRefRepository.save(ref);

        Quiz quiz = ref.getQuiz();
        if (correct) quiz.setCorrectAnswers(quiz.getCorrectAnswers() + 1);

        List<QuestionRef> allRefs = questionRefRepository.findByQuizId(quiz.getId());
        long answered = allRefs.stream().filter(q -> q.getUserAnswer() != null).count();
        boolean quizComplete = answered == quiz.getTotalQuestions() && !quiz.isCompleted();

        // Running score so far — only actually final/meaningful once
        // quizComplete is true, but the formula is the same either way since
        // answered == totalQuestions exactly when the attempt finishes.
        int scorePct = (int) Math.round((quiz.getCorrectAnswers() * 100.0) / answered);

        if (quizComplete) {
            quiz.setScorePct(scorePct);
            quiz.setCompleted(true);
            quiz.setCompletedAt(Instant.now());
        }
        quizRepository.save(quiz);

        // Publish on EVERY answer, not just the last one — this is what
        // actually drives the student's mastery update for the topic this
        // question tested. Gating it behind "quiz finished" used to mean
        // only the final question of any quiz ever counted; every earlier
        // answer in the same attempt was silently dropped from mastery
        // tracking. quizComplete tells notification-service which single
        // event (the last) is the one to actually notify on.
        eventProducer.publishQuizCompleted(QuizCompletedEvent.of(
                quiz.getId().toString(),
                quiz.getUserId().toString(),
                quiz.getSessionId().toString(),
                quiz.getSubject(),
                quiz.getTopic() != null ? quiz.getTopic() : quiz.getSubject(),
                correct,
                quiz.getDifficulty(),
                scorePct,
                quizComplete
        ));

        return new AnswerResultResponse(correct, ref.getCorrectAnswer(), ref.getExplanation());
    }

    @Override
    @Transactional(readOnly = true)
    public QuizResponse getQuiz(UUID quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + quizId));
        List<QuestionRef> refs = questionRefRepository.findByQuizId(quizId);
        return toResponse(quiz, refs);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizResponse> getHistory(UUID userId) {
        return quizRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(quiz -> toResponse(quiz,
                        questionRefRepository.findByQuizId(quiz.getId())))
                .toList();
    }

    private QuizResponse toResponse(Quiz quiz, List<QuestionRef> refs) {
        List<QuizQuestionResponse> questions = refs.stream().map(ref -> {
            List<String> options;
            try {
                options = objectMapper.readValue(ref.getOptionsJson(),
                        new TypeReference<>() {});
            } catch (Exception e) {
                options = List.of();
            }
            return new QuizQuestionResponse(ref.getId(), ref.getQuestionText(), options, ref.getTopic());
        }).toList();

        return new QuizResponse(
                quiz.getId(), quiz.getUserId(), quiz.getSessionId(),
                quiz.getSubject(), quiz.getTopic(), quiz.getDifficulty(),
                questions, quiz.getCreatedAt()
        );
    }
}
