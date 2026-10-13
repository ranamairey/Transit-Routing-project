import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { Api } from '../api/client.js'
import { useAuth } from '../context/AuthContext.jsx'
import { useI18n } from '../i18n/I18nContext.jsx'

export default function VerifyEmailPage() {
  const { t } = useI18n()
  const { user, verifyEmail } = useAuth()
  const [params] = useSearchParams()
  const token = params.get('token') || ''
  const [state, setState] = useState(token ? 'verifying' : 'idle')
  const [error, setError] = useState('')
  const [resent, setResent] = useState(false)

  useEffect(() => {
    if (!token) return
    verifyEmail(token).then(() => setState('done')).catch((err) => { setError(err.status === 400 ? t('auth.badLink') : err.message); setState('error') })
  }, [token]) // eslint-disable-line react-hooks/exhaustive-deps

  const resend = async () => { try { await Api.resendVerification(); setResent(true) } catch (err) { setError(err.message) } }

  return (
    <div className="page auth">
      <div className="panel">
        <h1 style={{ fontSize: '1.3rem' }}>{t('auth.verifyTitle')}</h1>
        {state === 'verifying' && <p>{t('auth.verifying')}</p>}
        {state === 'done' && <p className="ok">{t('auth.verifyDone')}</p>}
        {state === 'error' && <p className="error">{error}</p>}
        {state === 'idle' && user && !user.emailVerified && (
          <>
            <p className="muted">{t('auth.verifyNeeded')}</p>
            {resent ? <p className="ok">{t('auth.resent')}</p> : <button className="primary" onClick={resend}>{t('auth.resend')}</button>}
            {error && <p className="error">{error}</p>}
          </>
        )}
        {state === 'idle' && (!user || user.emailVerified) && <p className="ok">{t('auth.verifyDone')}</p>}
        <p style={{ marginTop: '0.75rem' }}><Link to="/">{t('auth.backHome')}</Link></p>
      </div>
    </div>
  )
}
