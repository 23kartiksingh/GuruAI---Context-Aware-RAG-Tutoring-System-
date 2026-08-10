# GuruAI — Context-Aware Adaptive Tutoring System
## Java Spring Boot 4.1 · Spring AI 2.0 · Kafka · Redis · pgvector · Docker

> A microservices rewrite of the original GuruAI Python monolith — AI-native,
> event-driven, and built around a dual-provider RAG pipeline (Groq + Gemini).
> Both backend and frontend are complete and verified end-to-end.

---

## What GuruAI actually does

A generic LLM chatbot treats every question the same way, forgets who
you are between sessions, and has to ask "what's your level?" every single
time. GuruAI is built to fix exactly that — personalized tutoring a plain
LLM wrapper can't do out of the box:

- **Fewer hallucinations.** Answers are grounded in *your* uploaded
  documents, not general web knowledge — retrieval is graded for relevance
  before the model ever sees it, so an irrelevant chunk can't quietly derail
  the answer, and responses cite "[Your Documents]" when they're actually
  using them.
- **Explanations at your pace, automatically.** GuruAI already tracks how
  well you know each topic, so it adjusts explanation depth on its own —
  no re-asking "how advanced should I go?" every conversation.
- **Quizzes that already know where you stand.** Difficulty is picked *for
  you* per topic, from your real quiz/flashcard history — not a fixed level
  you have to self-report.
- **Progress tracked topic by topic, not just overall.** Every quiz answer
  and flashcard review updates a per-topic mastery score (weighted toward
  recent performance), and if one starts slipping, GuruAI proactively nudges
  you to revisit it — before you'd have thought to ask.
- **Personalized, not generic.** Tell it once that you learn best from
  visual examples, or that you're into a specific hobby it can draw analogies
  from — it remembers, and folds that into how it teaches from then on.
- **Flashcards that keep up.** Every upload auto-generates spaced-repetition
  cards (SM-2, the same algorithm behind Anki) matched to your material.

Underneath, it's nine event-driven Spring Boot microservices over Kafka —
not one monolith — with two AI providers split by workload so no single
free-tier quota becomes a bottleneck.

---

## Architecture at a Glance

```
React :3000 → API Gateway :8080 → 9 microservices → 1 Postgres (8 DBs) + Kafka + Redis
```

| Service | Port | DB | Key Tech |
|---------|------|----|---------|
| API Gateway | 8080 | — | Spring Cloud Gateway (WebFlux), JWT filter, Redis rate limit |
| Auth Service | 8081 | auth_db | Spring Security 7, BCrypt, JWT access + refresh tokens, Google OAuth2 login/signup |
| Document Service | 8082 | document_db + pgvector | Apache Tika, Gemini embeddings, hybrid (dense + BM25) RRF search |
| Study Agent | 8083 | agent_db | CRAG pipeline, ReAct-style agent tools, chat sessions |
| Knowledge Service | 8085 | knowledge_db | EMA (α=0.3) mastery tracking, fuzzy topic matching |
| Quiz Service | 8086 | quiz_db | Groq structured-output MCQ generation, adaptive difficulty |
| Flashcard Service | 8087 | flashcard_db | SM-2 spaced repetition, auto-generation on document upload |
| User Memory | 8088 | memory_db | AI preference extraction from freeform chat |
| Notification | 8090 | notif_db | WebSocket push, Kafka domain-event consumers |
| Frontend | 3000 | — | React 19 + TS + Tailwind 4, served via nginx in Docker |

Study Agent absorbed a separate planned chat-service into one during the
11 → 9 consolidation. (An earlier AI learning-path planner endpoint also
lived here — built and working, but never given a frontend page — and was
removed as unused.)

---

## Quick Start

### Prerequisites
- Docker Desktop 4.x+ (running)
- Java 21 JDK
- Maven 3.9+
- A free [Groq API key](https://console.groq.com) and [Google Gemini API key](https://aistudio.google.com/app/apikey)

### 1. Configure environment
```bash
cd guruai-microservices
cp .env.example .env
# Edit .env: GROQ_API_KEY, GOOGLE_API_KEY, JWT_SECRET, INTERNAL_SERVICE_SECRET
```

### 2a. Development — infra only, run services from your IDE
```bash
docker-compose -f docker-compose.infra.yml up -d
```
Starts Postgres (8 databases, pgvector-enabled), Redis, Kafka (KRaft mode),
plus Redis Commander (`:8082`, admin/admin123) and Kafka UI (`:9000`, admin/admin123).
Then run each service from your IDE with the `local` profile.

### 2b. Staging/integration — everything in Docker
```bash
docker-compose up -d --build
```
Builds and starts the gateway, all 9 backend services, and the frontend
(`localhost:3000`) on top of the same infra stack.

### 2c. Frontend only, against a locally-running gateway
```bash
cd frontend
npm install
cp .env.example .env.local   # defaults already point at localhost:8080
npm run dev                   # localhost:3000, hot reload
```

### 3. Verify
```bash
docker-compose ps
```
All containers should show `Up (healthy)`. Confirm the 8 Kafka topics exist
via the Kafka UI at `localhost:9000`:
```
user.registered
document.indexed
chat.message.saved
quiz.completed
flashcard.reviewed
mastery.dropped
session.deleted
weak.topic.reminder
```

### 4. Build common-lib first if running from IDE
```bash
mvn clean install -pl common-lib
```

---

## Project Structure

```
guruai-microservices/
├── pom.xml                    ← Maven parent (Spring Boot 4.1.0, Java 21)
├── common-lib/                ← Shared DTOs, Kafka event contracts, enums, exceptions
├── api-gateway/                ← :8080 — routing, JWT validation, rate limiting
├── auth-service/                ← :8081 — register, login, JWT issuance/refresh
├── document-service/            ← :8082 — upload, Tika parse, Gemini embed, hybrid search
├── study-agent-service/         ← :8083 — CRAG chat + agent tools
├── knowledge-service/           ← :8085 — EMA mastery tracking
├── quiz-service/                ← :8086 — adaptive MCQ generation
├── flashcard-service/           ← :8087 — SM-2 spaced repetition
├── user-memory-service/         ← :8088 — persistent preference memory
├── notification-service/        ← :8090 — WebSocket push + Kafka consumers
├── frontend/                    ← :3000 — React + Vite + TypeScript + Tailwind
├── scripts/
│   └── init-databases.sql      ← Creates the 8 per-service databases on first boot
├── docker-compose.infra.yml    ← Infra only (dev mode)
├── docker-compose.yml          ← Infra + all 9 services (staging/integration)
├── .env.example                 ← Copy to .env and fill in values
└── README.md
```

---

## Database Layout

One PostgreSQL 16 (pgvector) instance on `localhost:5432` hosts eight
independent databases — one per service, giving schema isolation without the
overhead of eight separate server processes:

`auth_db`, `document_db`, `agent_db`, `knowledge_db`, `quiz_db`,
`flashcard_db`, `memory_db`, `notif_db`

Credentials: `${DB_USER}` / `${DB_PASSWORD}` from `.env` (defaults `guruai` /
`guruai_secret_2026`). Each service migrates its own schema via Flyway on
startup.

---

## AI Provider Split

Two providers, split deliberately across two free-tier quotas rather than
routing everything through one:

- **Groq** (`llama-3.1-8b-instant`, OpenAI-compatible API) — all
  conversation-time inference: tutor chat, CRAG relevance grading, quiz
  generation, flashcard generation, memory preference extraction. Used by
  study-agent, quiz, flashcard, and user-memory services.
- **Google Gemini** — indexing-time only, inside document-service:
  `gemini-embedding-001` for embeddings (Matryoshka-truncated to 768 dims to
  match the pgvector column) and `gemini-2.5-flash` for one-shot topic
  extraction per upload.

---

## Kafka Event Flow

8 topics, all consumed directly by domain services (no internal
re-broadcast topic):

| Topic | Producer | Consumers |
|-------|----------|-----------|
| `user.registered` | auth-service | knowledge-service, user-memory-service, notification-service |
| `document.indexed` | document-service | knowledge-service, flashcard-service, notification-service |
| `chat.message.saved` | study-agent-service | knowledge-service (activity signal only — see class javadoc) |
| `quiz.completed` | quiz-service | knowledge-service (mastery, fires per question), notification-service (result toast, fires once per completed attempt) |
| `flashcard.reviewed` | flashcard-service | — |
| `mastery.dropped` | knowledge-service | notification-service |
| `session.deleted` | study-agent-service | document-service, flashcard-service, knowledge-service (each drops its own session-scoped data) |
| `weak.topic.reminder` | knowledge-service (`WeakTopicReminderScheduler`, periodic) | notification-service |

---

## Frontend

React 19 + TypeScript + Vite + Tailwind 4, deliberately kept to plain
`useState`/`useContext` — no Redux/React Query/form libraries. Talks
exclusively to the API gateway (never a downstream service directly), with
an axios client that auto-refreshes the JWT via the httpOnly refresh-token
cookie on a 401.

| Page | Talks to |
|------|----------|
| Login / Register | auth-service (password login, plus "Continue with Google" OAuth2) |
| Documents | document-service (upload, status polling) + study-agent-service (sessions) |
| Chat | study-agent-service (CRAG chat, blocking — not the SSE stream endpoint) |
| Quiz | quiz-service (generate, per-question answer + explanation) |
| Flashcards | flashcard-service (SM-2 review; cards are auto-generated on upload, not created manually) |
| Knowledge Map | knowledge-service (per-topic mastery breakdown) |
| Dashboard | knowledge-service (mastery profile) + notification-service + user-memory-service ("About You" preferences — view/edit/delete) |

Built with Docker in mind: `Dockerfile` is a multi-stage `node` build →
`nginx` serve, with `VITE_*` values passed as Docker build args (Vite
inlines them into the bundle at build time, so they can't be container
`environment:` vars). See `frontend/README.md` for local dev setup.

---

## Development UIs

| Tool | URL | Credentials |
|------|-----|-------------|
| Redis Commander | http://localhost:8082 | admin / admin123 |
| Kafka UI | http://localhost:9000 | admin / admin123 |

---

## Version Notes

| Dependency | Version |
|-----------|---------|
| Spring Boot | 4.1.0 (Spring Framework 7.x) |
| Java | 21 (LTS) |
| Spring AI | 2.0.0 |
| Spring Cloud | 2025.1.1 (2025.0.x targets Boot 3.5 and fails to start on Boot 4) |
| pgvector/pgvector | pg16 |

---

## Status

Feature-complete, backend and frontend. Backend verified with a full
end-to-end walkthrough: register → login → upload document → RAG chat →
quiz generation → quiz completion → mastery update → notifications, plus
flashcard auto-generation and memory preference extraction. All 9 services
build and run stably in Docker on an 8GB dev machine, and pass integration
testing. The frontend covers every one of those flows (auth, documents,
chat, quiz, flashcards, dashboard) and type-checks/builds/lints clean.
