-- ================================================================
-- Quiz Service — Flyway V1 Migration
-- Creates: quizzes, question_refs
-- ================================================================

CREATE TABLE quizzes (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID         NOT NULL,
    session_id       UUID         NOT NULL,
    subject          VARCHAR(200) NOT NULL,
    topic            VARCHAR(300),
    -- Must be a DifficultyLevel enum name (BEGINNER/INTERMEDIATE/ADVANCED) —
    -- the entity reads this with @Enumerated(STRING), so a default like the
    -- old 'MEDIUM' (not an enum constant) would crash on read.
    difficulty       VARCHAR(20)  NOT NULL DEFAULT 'INTERMEDIATE',
    score_pct        INT,
    total_questions  INT          NOT NULL DEFAULT 0,
    correct_answers  INT          NOT NULL DEFAULT 0,
    completed        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at     TIMESTAMPTZ
);

CREATE INDEX idx_quizzes_user_id ON quizzes(user_id);
CREATE INDEX idx_quizzes_session_id ON quizzes(session_id);

CREATE TABLE question_refs (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    quiz_id         UUID         NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    question_text   TEXT         NOT NULL,
    options_json    TEXT         NOT NULL,
    correct_answer  VARCHAR(1)   NOT NULL,
    explanation     TEXT,
    topic           VARCHAR(300),
    user_answer     VARCHAR(1),
    is_correct      BOOLEAN,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_question_refs_quiz_id ON question_refs(quiz_id);
