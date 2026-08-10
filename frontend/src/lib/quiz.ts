import { api, unwrap } from '@/lib/api'
import type { AnswerResult, ApiResponse, GenerateQuizRequest, Quiz, SubmitAnswerRequest } from '@/types/api'

export function generateQuiz(body: GenerateQuizRequest) {
  return unwrap(api.post<ApiResponse<Quiz>>('/quizzes/generate', body))
}

export function submitAnswer(questionId: string, body: SubmitAnswerRequest) {
  return unwrap(api.post<ApiResponse<AnswerResult>>(`/quizzes/questions/${questionId}/answer`, body))
}
