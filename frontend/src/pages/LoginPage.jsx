import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { useI18n } from '../i18n/I18nContext.jsx'

export default function LoginPage() {
  const { login } = useAuth()
  const { t } = useI18n()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true); setError('')
    try {
      await login(email, password)
      navigate(location.state?.from || '/', { replace: true })
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page auth">
      <div className="panel">
        <h1 style={{ fontSize: '1.3rem' }}>{t('auth.loginTitle')}</h1>
        <p className="muted">{t('auth.loginHint')}</p>
        <form onSubmit={submit}>
          <div><label>{t('auth.email')}</label><input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} /></div>
          <div><label>{t('auth.password')}</label><input type="password" required value={password} onChange={(e) => setPassword(e.target.value)} /></div>
          {error && <p className="error">{error}</p>}
          <button type="submit" className="primary" disabled={busy}>{busy ? '…' : t('auth.login')}</button>
        </form>
        <p style={{ marginTop: '0.75rem' }}>{t('auth.noAccount')} <Link to="/register">{t('auth.createAccount')}</Link> · <Link to="/forgot">{t('auth.forgot')}</Link></p>
      </div>
    </div>
  )
}
