import { Route, Routes } from 'react-router'
import LoginPage from '@/auth/LoginPage'
import { RequireAuth } from '@/auth/RequireAuth'
import { AppShell } from '@/components/layout/AppShell'
import CreditsPage from '@/pages/CreditsPage'
import DashboardPage from '@/pages/DashboardPage'
import GeneratePage from '@/pages/GeneratePage'
import IdeaDetailPage from '@/pages/IdeaDetailPage'
import IdeasPage from '@/pages/IdeasPage'
import NotFoundPage from '@/pages/NotFoundPage'
import WorkPage from '@/pages/WorkPage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <RequireAuth>
            <AppShell />
          </RequireAuth>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route path="generate" element={<GeneratePage />} />
        <Route path="ideas" element={<IdeasPage />} />
        <Route path="ideas/:id" element={<IdeaDetailPage />} />
        <Route path="ideas/:id/work" element={<WorkPage />} />
        <Route path="credits" element={<CreditsPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
