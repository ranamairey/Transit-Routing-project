import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { useI18n } from '../i18n/I18nContext.jsx'

export default function LoginPage() {
  const { user, isAdmin, login } = useAuth()
  const { t, toggle } = useI18n()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  if (user && isAdmin) return <Navigate to="/" replace />

  const submit = async (e) => {
    e.preventDefault(); setBusy(true); setError('')
    try { await login(email, password); navigate('/', { replace: true }) }
    catch (err) { setError(err.code === 'NOT_ADMIN' ? t('login.notAdmin') : err.message) }
    finally { setBusy(false) }
  }

  return (
    <div className="login-wrap">
      <div className="login-card">
        <div className="brand"><div className="brand-mark">⌁</div><div><b>{t('brand.name')}</b><span>{t('brand.sub')}</span></div></div>
        <p className="eyebrow">{t('login.title')}</p>
        <p className="description" style={{ marginBottom: 22 }}>{t('login.hint')}</p>
        <form onSubmit={submit}>
          <label className="field-label" htmlFor="email">{t('login.email')}</label>
          <input className="field" id="email" type="email" required autoComplete="username" value={email} onChange={(e) => setEmail(e.target.value)} dir="ltr" />
          <label className="field-label" htmlFor="password">{t('login.password')}</label>
          <input className="field" id="password" type="password" required autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} />
          {error && <p className="error" style={{ marginTop: -8 }}>{error}</p>}
          <div className="form-actions" style={{ justifyContent: 'space-between', alignItems: 'center' }}>
            <button type="button" className="button secondary small" onClick={toggle}>🌐 {t('nav.lang')}</button>
            <button type="submit" className="button" disabled={busy}>{busy ? '…' : t('login.submit')}</button>
          </div>
        </form>
      </div>
    </div>
  )
}
