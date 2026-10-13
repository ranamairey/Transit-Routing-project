import { Navigate, Route, Routes } from 'react-router-dom'
import NavBar from './components/NavBar.jsx'
import { useAuth } from './context/AuthContext.jsx'
import { useNetwork } from './hooks/useNetwork.js'
import { useI18n } from './i18n/I18nContext.jsx'
import PlannerPage from './pages/PlannerPage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import RegisterPage from './pages/RegisterPage.jsx'
import AdminLayout from './pages/admin/AdminLayout.jsx'
import AdminStations from './pages/admin/AdminStations.jsx'
import AdminRoutes from './pages/admin/AdminRoutes.jsx'
import AdminReports from './pages/admin/AdminReports.jsx'

function RequireAdmin({ children }) {
  const { user, isAdmin } = useAuth()
  const { t } = useI18n()
  if (!user) return <Navigate to="/login" replace state={{ from: '/admin' }} />
  if (!isAdmin) return <div className="page"><p className="error">{t('auth.adminOnly')}</p></div>
  return children
}

export default function App() {
  const net = useNetwork()
  const { t } = useI18n()
  return (
    <div className="app">
      <NavBar offline={net.offline} />
      {net.offline && <div className="offline">{t('offlineBanner')}</div>}
      <Routes>
        <Route path="/" element={<PlannerPage net={net} />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/admin" element={<RequireAdmin><AdminLayout /></RequireAdmin>}>
          <Route index element={<Navigate to="reports" replace />} />
          <Route path="reports" element={<AdminReports net={net} />} />
          <Route path="stations" element={<AdminStations net={net} />} />
          <Route path="routes" element={<AdminRoutes net={net} />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  )
}
