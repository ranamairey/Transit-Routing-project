import { Navigate, Route, Routes } from 'react-router-dom'
import NavBar from './components/NavBar.jsx'
import { useNetwork } from './hooks/useNetwork.js'
import { useI18n } from './i18n/I18nContext.jsx'
import PlannerPage from './pages/PlannerPage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import RegisterPage from './pages/RegisterPage.jsx'

/** Rider app: planner, login/register and reporting. Administration lives in the separate admin console. */
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
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  )
}
