import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './context/AuthContext.jsx'
import Layout from './components/Layout.jsx'
import LoginPage from './pages/LoginPage.jsx'
import DashboardPage from './pages/DashboardPage.jsx'
import StationsPage from './pages/StationsPage.jsx'
import RoutesPage from './pages/RoutesPage.jsx'
import ReportsPage from './pages/ReportsPage.jsx'
import UsersPage from './pages/UsersPage.jsx'
import DataPage from './pages/DataPage.jsx'

function RequireAdmin({ children }) {
  const { user, isAdmin } = useAuth()
  if (!user || !isAdmin) return <Navigate to="/login" replace />
  return children
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={<RequireAdmin><Layout /></RequireAdmin>}>
        <Route index element={<DashboardPage />} />
        <Route path="stations" element={<StationsPage />} />
        <Route path="routes" element={<RoutesPage />} />
        <Route path="reports" element={<ReportsPage />} />
        <Route path="users" element={<UsersPage />} />
        <Route path="data" element={<DataPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
