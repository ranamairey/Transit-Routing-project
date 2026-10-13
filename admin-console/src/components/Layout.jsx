import { useEffect, useState } from 'react'
import { NavLink, Outlet, Link } from 'react-router-dom'
import { Api } from '../api/client.js'
import { useAuth } from '../context/AuthContext.jsx'
import { useI18n } from '../i18n/I18nContext.jsx'

export default function Layout() {
  const { user, logout } = useAuth()
  const { t, toggle } = useI18n()
  const [health, setHealth] = useState(null)
  useEffect(() => {
    let alive = true
    const check = () => Api.health().then((h) => alive && setHealth(h.status === 'UP')).catch(() => alive && setHealth(false))
    check()
    const id = setInterval(check, 30000)
    return () => { alive = false; clearInterval(id) }
  }, [])
  return (
    <div className="console">
      <aside className="sidebar">
        <Link to="/" className="brand">🛠 {t('appName')}</Link>
        <NavLink to="/" end>📊 {t('nav.dashboard')}</NavLink>
        <NavLink to="/reports">⚠️ {t('nav.reports')}</NavLink>
        <NavLink to="/stations">📍 {t('nav.stations')}</NavLink>
        <NavLink to="/routes">🚌 {t('nav.routes')}</NavLink>
        <NavLink to="/users">👥 {t('nav.users')}</NavLink>
        <NavLink to="/data">🗂 {t('nav.data')}</NavLink>
        <span className="grow" />
        <div className="me">{t('nav.api')}: <span className={`health ${health ? 'up' : 'down'}`}>{health == null ? '…' : health ? 'UP' : 'DOWN'}</span></div>
        <div className="me">{user?.fullName}<br /><small>{user?.email}</small></div>
        <button className="small" onClick={toggle}>🌐 {t('nav.lang')}</button>
        <button className="small" onClick={logout}>{t('nav.logout')}</button>
      </aside>
      <main className="content"><Outlet /></main>
    </div>
  )
}
