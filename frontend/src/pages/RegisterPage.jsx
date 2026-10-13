import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { useI18n } from '../i18n/I18nContext.jsx'

export default function RegisterPage() {
  const { register } = useAuth()
  const { t } = useI18n()
  const navigate = useNavigate()
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true); setError('')
    try {
      await register(fullName, email, password)
      navigate('/', { replace: true })
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page auth">
      <div className="panel">
        <h1 style={{ fontSize: '1.3rem' }}>{t('auth.registerTitle')}</h1>
        <form onSubmit={submit}>
          <div><label>{t('auth.fullName')}</label><input required value={fullName} onChange={(e) => setFullName(e.target.value)} /></div>
          <div><label>{t('auth.email')}</label><input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} /></div>
          <div><label>{t('auth.passwordHint')}</label><input type="password" minLength={8} required value={password} onChange={(e) => setPassword(e.target.value)} /></div>
          {error && <p className="error">{error}</p>}
          <button type="submit" className="primary" disabled={busy}>{busy ? '…' : t('auth.register')}</button>
        </form>
        <p style={{ marginTop: '0.75rem' }}>{t('auth.haveAccount')} <Link to="/login">{t('auth.signIn')}</Link></p>
      </div>
    </div>
  )
}
