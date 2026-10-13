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
    e.preventDefault()
    setBusy(true); setError('')
    try {
      await login(email, password)
      navigate('/', { replace: true })
    } catch (err) {
      setError(err.code === 'NOT_ADMIN' ? t('login.notAdmin') : err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page auth">
      <div className="card">
        <div className="row between">
          <h1 style={{ fontSize: '1.3rem' }}>🛠 {t('login.title')}</h1>
          <button className="secondary small" onClick={toggle}>🌐 {t('nav.lang')}</button>
        </div>
        <p className="muted">{t('login.hint')}</p>
        <form onSubmit={submit}>
          <div className="field"><label>{t('login.email')}</label><input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} /></div>
          <div className="field"><label>{t('login.password')}</label><input type="password" required value={password} onChange={(e) => setPassword(e.target.value)} /></div>
          {error && <p className="error">{error}</p>}
          <button type="submit" disabled={busy} style={{ width: '100%' }}>{busy ? '…' : t('login.submit')}</button>
        </form>
      </div>
    </div>
  )
}
