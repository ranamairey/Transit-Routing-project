import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Api } from '../api/client.js'
import { useAuth } from '../context/AuthContext.jsx'
import { useRider } from '../context/RiderContext.jsx'
import { useI18n } from '../i18n/I18nContext.jsx'
import { nameOf } from '../utils/names.js'
import LiveBadge from './LiveBadge.jsx'
import { useToast } from './Toast.jsx'

const PUSH_KEY = 'sst.pushEndpoint'
const supported = () => typeof window !== 'undefined' && 'Notification' in window && 'serviceWorker' in navigator

function urlBase64ToUint8Array(base64) {
  const padded = (base64 + '='.repeat((4 - (base64.length % 4)) % 4)).replace(/-/g, '+').replace(/_/g, '/')
  return Uint8Array.from(atob(padded), (c) => c.charCodeAt(0))
}

/** Bell in the top bar: problems on the lines the rider follows, and the switch for device notifications. */
export default function AlertsBell({ routes = [] }) {
  const { t, lang, timeAgo } = useI18n()
  const { user } = useAuth()
  const notify = useToast()
  const { alerts, unread, markSeen, follows } = useRider()
  const [open, setOpen] = useState(false)
  const [permission, setPermission] = useState(() => (supported() ? Notification.permission : 'unsupported'))
  const byId = Object.fromEntries(routes.map((r) => [r.id, r]))

  // Background alerts (app closed) need a push subscription on the account that follows the lines.
  const subscribePush = useCallback(async () => {
    if (!user || !('PushManager' in window)) return false
    const key = await Api.push.key().catch(() => null)
    if (!key?.enabled) return false
    const reg = await navigator.serviceWorker.ready
    const sub = (await reg.pushManager.getSubscription())
      || await reg.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: urlBase64ToUint8Array(key.publicKey) })
    const json = sub.toJSON()
    await Api.push.subscribe({ endpoint: json.endpoint, p256dh: json.keys.p256dh, auth: json.keys.auth })
    try { localStorage.setItem(PUSH_KEY, json.endpoint) } catch { /* ignore */ }
    return true
  }, [user])

  const enable = async () => {
    if (!supported()) return
    const result = await Notification.requestPermission()
    setPermission(result)
    if (result !== 'granted') return
    try { await subscribePush() } catch { /* in-app alerts still work */ }
    notify(t('bell.enabled'))
  }

  // Keep the subscription attached to whoever is signed in.
  useEffect(() => {
    if (permission === 'granted' && user) subscribePush().catch(() => {})
  }, [permission, user, subscribePush])

  // A new alert while the app is open: system notification when the app is in the background, toast otherwise.
  useEffect(() => {
    const onAlert = async (e) => {
      const a = e.detail
      const name = nameOf(byId[a.routeId], lang) || ''
      const body = a.status !== 'NORMAL' ? t(`routeStatus.${a.status}`) : t('bell.reports', { n: Object.values(a.reportCounts || {}).reduce((x, y) => x + y, 0) })
      if (document.visibilityState === 'visible' || permission !== 'granted') { notify(`${t('bell.notifTitle', { route: name })} — ${body}`); return }
      try {
        const reg = await navigator.serviceWorker.ready
        reg.showNotification(t('bell.notifTitle', { route: name }), { body, tag: `line-${a.routeId}`, data: { url: `${import.meta.env.BASE_URL}lines/${a.routeId}` } })
      } catch { /* ignore */ }
    }
    window.addEventListener('sst:line-alert', onAlert)
    return () => window.removeEventListener('sst:line-alert', onAlert)
  })

  const toggle = () => { if (!open) markSeen(); setOpen(!open) }
  useEffect(() => {
    if (!open) return undefined
    const onKey = (e) => { if (e.key === 'Escape') setOpen(false) }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [open])

  return (
    <div className="bell-wrap">
      <button className="bell" onClick={toggle} aria-label={t('nav2.alerts')} aria-expanded={open}>
        🔔{unread > 0 && <span className="bell-count">{unread}</span>}
      </button>
      {open && (
        <>
          <div className="bell-backdrop" onClick={() => setOpen(false)} />
          <div className="bell-menu" role="dialog" aria-label={t('bell.title')}>
            <b>{t('bell.title')}</b>
            {follows.size === 0 ? <p className="sub">{t('bell.noFollows')}</p>
              : alerts.length === 0 ? <p className="sub">{t('bell.empty')}</p> : (
                <ul>
                  {alerts.map((a) => (
                    <li key={a.routeId}>
                      <Link to={`/lines/${a.routeId}`} onClick={() => setOpen(false)}>{nameOf(byId[a.routeId], lang) || a.routeId}</Link>
                      <LiveBadge live={a} />
                      {(a.lastReportAt || a.statusUpdatedAt) && <small>{timeAgo(a.lastReportAt || a.statusUpdatedAt)}</small>}
                    </li>
                  ))}
                </ul>
              )}
            <div className="bell-foot">
              {permission === 'unsupported' ? <p className="sub">{t('bell.unsupported')}</p>
                : permission === 'denied' ? <p className="sub">{t('bell.denied')}</p>
                  : permission === 'granted' ? <p className="sub ok">✓ {t('bell.enabled')}{!user && <> — {t('bell.needLogin')}</>}</p>
                    : <button className="btn" onClick={enable}>{t('bell.enable')}</button>}
              {permission === 'default' && !user && <p className="sub">{t('bell.needLogin')}</p>}
            </div>
          </div>
        </>
      )}
    </div>
  )
}
