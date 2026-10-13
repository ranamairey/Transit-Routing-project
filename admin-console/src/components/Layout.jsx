import { useEffect, useState } from 'react'
import { NavLink, Outlet, Link, useLocation, useNavigate } from 'react-router-dom'
import { Api } from '../api/client.js'
import { useAuth } from '../context/AuthContext.jsx'
import { useI18n } from '../i18n/I18nContext.jsx'
import { Initials } from './ui.jsx'

const NAV = [
  { to: '/', key: 'dashboard', ico: '▦', end: true },
  { to: '/reports', key: 'reports', ico: '◫', count: 'pending' },
  { to: '/stations', key: 'stations', ico: '♧' },
  { to: '/routes', key: 'routes', ico: '≋' },
  { to: '/users', key: 'users', ico: '♙', count: 'users' },
  { to: '/data', key: 'data', ico: '⇅' },
]

export default function Layout() {
  const { user, logout } = useAuth()
  const { t, toggle } = useI18n()
  const location = useLocation()
  const navigate = useNavigate()
  const [health, setHealth] = useState(null)
  const [counts, setCounts] = useState({})
  const [quick, setQuick] = useState('')

  useEffect(() => {
    let alive = true
    const check = () => {
      Api.health().then((h) => alive && setHealth(h.status === 'UP')).catch(() => alive && setHealth(false))
      Api.stats().then((s) => alive && setCounts({ pending: s.reportsPending, users: s.users })).catch(() => {})
    }
    check()
    const id = setInterval(check, 30000)
    return () => { alive = false; clearInterval(id) }
  }, [location.pathname])

  useEffect(() => {
    const onKey = (e) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') { e.preventDefault(); document.getElementById('quick-search')?.focus() }
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [])

  const current = NAV.find((n) => (n.end ? location.pathname === n.to : location.pathname.startsWith(n.to))) || NAV[0]
  const runQuick = (e) => {
    e.preventDefault()
    if (quick.trim()) navigate(`/stations?q=${encodeURIComponent(quick.trim())}`)
  }

  return (
    <div className="app">
      <aside className="sidebar">
        <Link to="/" className="brand"><div className="brand-mark">⌁</div><div><b>{t('brand.name')}</b><span>{t('brand.sub')}</span></div></Link>
        <div className="section-label">{t('nav.workspace')}</div>
        <nav className="nav">
          {NAV.map((n) => (
            <NavLink key={n.to} to={n.to} end={n.end} className="nav-link">
              <span className="nav-ico">{n.ico}</span><span className="nav-name">{t(`nav.${n.key}`)}</span>
              {n.count && counts[n.count] > 0 && <span className="count">{counts[n.count]}</span>}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <div className="status"><span>{t('nav.systemStatus')}</span>
            <strong className={health === false ? 'down' : ''}><i className="dot" /> {health == null ? '…' : health ? t('nav.online') : t('nav.offline')}</strong></div>
          <div className="profile"><div className="avatar"><Initials name={user?.fullName} /></div><div><b>{user?.fullName}</b><small>{user?.email}</small></div></div>
          <div className="bottom-actions">
            <button onClick={toggle}>🌐 {t('nav.lang')}</button>
            <button onClick={logout}>↪ {t('nav.logout')}</button>
          </div>
        </div>
      </aside>
      <div className="main">
        <header className="topbar">
          <div className="breadcrumb">{t('nav.workspace')}<span className="sep">/</span><strong>{t(`nav.${current.key}`)}</strong></div>
          <div className="top-right">
            <form className="search-top" onSubmit={runQuick}>⌕ <input id="quick-search" value={quick} onChange={(e) => setQuick(e.target.value)} placeholder={t('nav.quickSearch')} /><span className="key">⌘K</span></form>
            <div className="top-avatar"><Initials name={user?.fullName} /></div>
          </div>
        </header>
        <main className="content"><Outlet /></main>
      </div>
    </div>
  )
}
