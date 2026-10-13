import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { useI18n } from '../i18n/I18nContext.jsx'

export default function ResetPasswordPage() {
  const { t } = useI18n()
  const { resetPassword } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const token = params.get('token') || ''
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [done, setDone] = useState(false)
  const submit = async (e) => {
    e.preventDefault(); setBusy(true); setError('')
    try { await resetPassword(token, password); setDone(true); setTimeout(() => navigate('/', { replace: true }), 1500) }
    catch (err) { setError(err.status === 400 ? t('auth.badLink') : err.message) } finally { setBusy(false) }
  }
  return (
    <div className="page auth">
      <div className="panel">
        <h1 style={{ fontSize: '1.3rem' }}>{t('auth.resetTitle')}</h1>
        {!token ? <p className="error">{t('auth.badLink')}</p> : done ? <p className="ok">{t('auth.resetDone')}</p> : (
          <form onSubmit={submit}>
            <div><label>{t('auth.newPassword')}</label><input type="password" minLength={6} required autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} /></div>
            {error && <p className="error">{error}</p>}
            <button type="submit" className="primary" disabled={busy}>{busy ? '…' : t('auth.resetBtn')}</button>
          </form>
        )}
        <p style={{ marginTop: '0.75rem' }}><Link to="/login">{t('auth.signIn')}</Link></p>
      </div>
    </div>
  )
}
