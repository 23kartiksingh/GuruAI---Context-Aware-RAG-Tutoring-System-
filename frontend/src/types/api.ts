// Mirrors com.guruai.common.dto.ApiResponse<T> — every service wraps
// responses in this envelope, so every API call on the frontend goes
// through the same shape.
export interface ApiResponse<T> {
  success: boolean
  status: number
  data: T | null
  error: string | null
  message: string | null
  timestamp: string
}

// Mirrors auth-service's AuthResponse. The access token also arrives as an
// httpOnly cookie, but we keep a copy here so it can be attached to the
// Authorization header on cross-origin XHR/fetch calls. The refresh token
// is cookie-only (never in the JSON body) — the browser sends it
// automatically on POST /auth/refresh as long as requests use credentials.
export interface AuthResponse {
  accessToken: string
  userId: string
  username: string
  name: string
  tokenType: string
  expiresIn: number
}

export interface ApiError {
  status: number
  message: string
}

// Mirrors auth-service's request DTOs (com.guruai.auth.dto.request.*).
export interface RegisterRequest {
  username: string
  password: string
  name?: string
}

export interface LoginRequest {
  username: string
  password: string
}

// Mirrors auth-service's UserProfileResponse.
export interface UserProfile {
  userId: string
  username: string
  name: string
  bio: string | null
  createdAt: string
}

// Mirrors study-agent-service's SessionResponse / CreateSessionRequest.
export interface StudySession {
  id: string
  userId: string
  title: string
  subject: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateSessionRequest {
  userId: string
  title: string
  subject?: string
}

// Mirrors document-service's DocumentResponse.
export type DocumentStatus = 'PROCESSING' | 'INDEXED' | 'FAILED'

export interface StudyDocument {
  documentId: string
  sessionId: string
  filename: string
  fileType: string
  fileSizeMb: number
  chunkCount: number
  status: DocumentStatus
  topics: string[]
  subject: string | null
  createdAt: string
}

// Mirrors study-agent-service's chat DTOs.
export interface ChatRequest {
  userId: string
  sessionId: string
  message: string
}

export interface ChatResponse {
  messageId: string
  reply: string
  usedRag: boolean
  timestamp: string
}

export interface ChatMessage {
  id: string
  role: string
  content: string
  createdAt: string
}

// Mirrors com.guruai.common.enums.DifficultyLevel.
export type DifficultyLevel = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'

// Mirrors quiz-service's DTOs.
export interface GenerateQuizRequest {
  userId: string
  sessionId: string
  subject: string
  topic?: string
  /** Omit to let the backend pick from the student's mastery ("Auto"). */
  difficulty?: DifficultyLevel
  questionCount: number
}

export interface QuizQuestion {
  id: string
  questionText: string
  options: string[]
  topic: string
}

export interface Quiz {
  quizId: string
  userId: string
  sessionId: string
  subject: string
  topic: string | null
  difficulty: DifficultyLevel
  questions: QuizQuestion[]
  createdAt: string
}

export interface SubmitAnswerRequest {
  questionId: string
  answer: 'A' | 'B' | 'C' | 'D'
}

export interface AnswerResult {
  correct: boolean
  correctAnswer: string
  explanation: string
}

// Mirrors flashcard-service's DTOs.
export interface Flashcard {
  id: string
  userId: string
  sessionId: string
  subject: string
  topic: string
  front: string
  back: string
  easeFactor: number
  intervalDays: number
  repetitions: number
  nextReviewDate: string
  createdAt: string
}

export interface ReviewResult {
  newEaseFactor: number
  newIntervalDays: number
  nextReviewDate: string
}

// Mirrors com.guruai.common.enums.MasteryLevel and knowledge-service DTOs.
export type MasteryLevel = 'WEAK' | 'AVERAGE' | 'STRONG'

export interface TopicMastery {
  id: string
  userId: string
  // Null for legacy pre-session-scoping rows or the neutral placeholder the
  // backend returns for a topic with no record yet.
  sessionId: string | null
  subject: string
  topic: string
  emaScore: number
  correctCount: number
  totalCount: number
  masteryLevel: MasteryLevel
  lastUpdated: string
}

export interface MasteryProfile {
  userId: string
  topics: TopicMastery[]
  totalTopics: number
  weakCount: number
  averageCount: number
  strongCount: number
  overallMasteryPct: number
}

export interface UserStats {
  userId: string
  totalQuestions: number
  correctAnswers: number
  avgMasteryPct: number
}

// Mirrors user-memory-service's MemoryItemsResponse. Preferences are stored
// per-user only (no sessionId) — common to every session/chat by design.
export interface MemoryItem {
  id: string
  text: string
}

export interface MemoryItemsResponse {
  items: MemoryItem[]
  count: number
}

// Mirrors notification-service's NotificationResponse.
export interface Notification {
  id: string
  userId: string
  type: string
  title: string
  message: string
  isRead: boolean
  // Deep-link target — non-null only for WEAK_TOPIC_REMINDER today.
  sessionId: string | null
  topic: string | null
  createdAt: string
}
