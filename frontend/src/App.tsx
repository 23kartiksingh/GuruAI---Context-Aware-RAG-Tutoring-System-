import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from '@/context/AuthContext'
import { SessionProvider } from '@/context/SessionContext'
import { NotificationProvider } from '@/context/NotificationContext'
import { ProtectedRoute } from '@/components/ProtectedRoute'
import { Layout } from '@/components/Layout'
import Landing from '@/pages/Landing'
import Login from '@/pages/Login'
import Register from '@/pages/Register'
import Dashboard from '@/pages/Dashboard'
import Documents from '@/pages/Documents'
import KnowledgeMap from '@/pages/KnowledgeMap'
import Chat from '@/pages/Chat'
import Quiz from '@/pages/Quiz'
import Flashcards from '@/pages/Flashcards'
import NotFound from '@/pages/NotFound'

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/" element={<Landing />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />

        <Route element={<ProtectedRoute />}>
          <Route
            element={
              <SessionProvider>
                <NotificationProvider>
                  <Layout />
                </NotificationProvider>
              </SessionProvider>
            }
          >
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/documents" element={<Documents />} />
            <Route path="/knowledge" element={<KnowledgeMap />} />
            <Route path="/chat" element={<Chat />} />
            <Route path="/quiz" element={<Quiz />} />
            <Route path="/flashcards" element={<Flashcards />} />
          </Route>
        </Route>

        <Route path="/404" element={<NotFound />} />
        <Route path="*" element={<Navigate to="/404" replace />} />
      </Routes>
    </AuthProvider>
  )
}
