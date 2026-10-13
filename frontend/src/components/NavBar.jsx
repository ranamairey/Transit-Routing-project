import { NavLink, Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { useI18n } from '../i18n/I18nContext.jsx'

const ADMIN_URL = import.meta.env.VITE_ADMIN_URL || ''

export default function NavBar({ offline }) {
  const { user, isAdmin, logout } = useAuth()
  const { t, toggle } = useI18n()
  return (
    <nav className="nav">
      <Link to="/" className="brand">🚌 {t('appName')}</Link>
      <NavLink to="/" end>{t('nav.planner')}</NavLink>
      {isAdmin && ADMIN_URL && <a href={ADMIN_URL} target="_blank" rel="noreferrer">{t('nav.admin')} ↗</a>}
      <span className="spacer" />
      {offline && <span className="status">{t('nav.offline')}</span>}
      <button className="lang small" onClick={toggle} title="Language / اللغة">🌐 {t('nav.lang')}</button>
      {user ? (
        <>
          <span>{user.fullName}</span>
          <button className="ghost" style={{ color: '#fff' }} onClick={logout}>{t('nav.logout')}</button>
        </>
      ) : (
        <NavLink to="/login">{t('nav.login')}</NavLink>
      )}
    </nav>
  )
}
