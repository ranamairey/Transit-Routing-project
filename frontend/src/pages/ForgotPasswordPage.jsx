import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Api } from '../api/client.js'
import { useI18n } from '../i18n/I18nContext.jsx'

export default function ForgotPasswordPage() {
  const { t } = useI18n()
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const submit = async (e) => {
    e.preventDefault(); setBusy(true); setError('')
    try { await Api.forgotPassword(email); setSent(true) } catch (err) { setError(err.message) } finally { setBusy(false) }
  }
  return (
    <div className="page auth">
      <div className="panel">
        <h1 style={{ fontSize: '1.3rem' }}>{t('auth.forgotTitle')}</h1>
        <p className="muted">{t('auth.forgotHint')}</p>
        {sent ? <p className="ok">{t('auth.forgotSent')}</p> : (
          <form onSubmit={submit}>
            <div><label>{t('auth.email')}</label><input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} dir="ltr" /></div>
            {error && <p className="error">{error}</p>}
            <button type="submit" className="primary" disabled={busy}>{busy ? '…' : t('auth.sendLink')}</button>
          </form>
        )}
        <p style={{ marginTop: '0.75rem' }}><Link to="/login">{t('auth.signIn')}</Link></p>
      </div>
    </div>
  )
}
