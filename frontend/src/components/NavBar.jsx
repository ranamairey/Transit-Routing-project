import { useState } from 'react'
import { NavLink, Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { useI18n } from '../i18n/I18nContext.jsx'

const ADMIN_URL = import.meta.env.VITE_ADMIN_URL || ''

export default function NavBar() {
  const { user, isAdmin, logout } = useAuth()
  const { t, toggle } = useI18n()
  const [about, setAbout] = useState(false)
  return (
    <header className="topbar">
      <Link to="/" className="brand"><div className="logo">🚌</div><div><b>{t('appName')}</b><small>{t('tagline')}</small></div></Link>
      <nav className="nav">
        <NavLink to="/" end>{t('nav.planner')}</NavLink>
        <button onClick={() => setAbout(true)}>{t('nav.about')}</button>
        {isAdmin && ADMIN_URL && <a href={ADMIN_URL} target="_blank" rel="noreferrer">{t('nav.admin')} ↗</a>}
        <button className="lang" onClick={toggle}>🌐 {t('nav.lang')}</button>
        {user ? (
          <>
            <span className="user">{user.fullName}</span>
            <button className="login" onClick={logout}>{t('nav.logout')}</button>
          </>
        ) : (
          <NavLink to="/login" className="login">{t('nav.login')} ←</NavLink>
        )}
      </nav>
      {about && (
        <div className="modal-backdrop" onClick={() => setAbout(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h2>{t('about.title')}</h2>
            <p className="sub" style={{ marginBottom: 12 }}>{t('about.body')}</p>
            <ul className="sub" style={{ paddingInlineStart: 18, marginBottom: 14 }}>
              <li>{t('about.p1')}</li><li>{t('about.p2')}</li><li>{t('about.p3')}</li>
            </ul>
            <button className="btn" onClick={() => setAbout(false)}>{t('report.close')}</button>
          </div>
        </div>
      )}
    </header>
  )
}
