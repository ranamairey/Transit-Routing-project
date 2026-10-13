import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Api } from '../api/client.js'
import MapView from '../components/MapView.jsx'
import { useNetwork } from '../hooks.js'
import { useI18n } from '../i18n/I18nContext.jsx'

function Bars({ data, labelFor }) {
  const entries = Object.entries(data || {})
  const max = Math.max(1, ...entries.map(([, v]) => v))
  if (entries.length === 0) return <p className="muted">—</p>
  return entries.map(([k, v]) => (
    <div className="bar" key={k}>
      <span style={{ minWidth: 130 }}>{labelFor(k)}</span>
      <div className="track"><div className="fill" style={{ width: `${(v / max) * 100}%` }} /></div>
      <b style={{ minWidth: 28, textAlign: 'end' }}>{v}</b>
    </div>
  ))
}

export default function DashboardPage() {
  const { t, timeAgo } = useI18n()
  const { stations, routes } = useNetwork()
  const [stats, setStats] = useState(null)
  const [recent, setRecent] = useState([])
  const [error, setError] = useState('')

  useEffect(() => {
    Promise.all([Api.stats(), Api.reports.list({ hours: 168, dismissed: false, size: 8 })])
      .then(([s, r]) => { setStats(s); setRecent(r.items) })
      .catch((e) => setError(e.message))
  }, [])

  const reportPins = recent.filter((r) => r.location).map((r) => ({
    id: r.id, name: `${t(`reportTypes.${r.reportType}`)} — ${r.routeName}`, location: r.location, stationKind: 'HUB',
  }))

  return (
    <div>
      <h1 style={{ fontSize: '1.4rem' }}>{t('dash.title')}</h1>
      {error && <p className="error">{error}</p>}
      {stats && (
        <div className="kpis">
          {[['stations', stats.stations], ['routes', stats.routes], ['users', stats.users], ['reports24', stats.reportsLast24h], ['pending', stats.reportsPending], ['reportsTotal', stats.reportsTotal]].map(([k, v]) => (
            <div className="kpi" key={k}><div className="value">{v}</div><div className="label">{t(`dash.${k}`)}</div></div>
          ))}
        </div>
      )}
      <div className="two">
        <div className="card"><h2 style={{ fontSize: '1.05rem' }}>{t('dash.byType')}</h2><Bars data={stats?.reportsByTypeLast24h} labelFor={(k) => t(`reportTypes.${k}`)} /></div>
        <div className="card"><h2 style={{ fontSize: '1.05rem' }}>{t('dash.byStatus')}</h2><Bars data={stats?.routesByStatus} labelFor={(k) => t(`routeStatus.${k}`)} /></div>
      </div>
      <div className="two">
        <div className="card">
          <h2 style={{ fontSize: '1.05rem' }}>{t('dash.map')}</h2>
          <div style={{ height: 380, borderRadius: 12, overflow: 'hidden' }}>
            <MapView stations={stations} routes={routes} origin={reportPins[0]?.location} />
          </div>
        </div>
        <div className="card">
          <div className="row between"><h2 style={{ fontSize: '1.05rem' }}>{t('dash.recent')}</h2><Link to="/reports">{t('dash.viewAll')} →</Link></div>
          <table>
            <thead><tr><th>{t('reports.time')}</th><th>{t('reports.route')}</th><th>{t('reports.type')}</th><th>{t('common.status')}</th></tr></thead>
            <tbody>
              {recent.map((r) => (
                <tr key={r.id}>
                  <td>{timeAgo(r.sentAt)}</td><td>{r.routeName}</td><td>{t(`reportTypes.${r.reportType}`)}</td>
                  <td>{r.applied ? <span className="badge FARE_UPDATED">{t('reports.applied')}</span> : <span className="badge">{t('reports.pendingReview')}</span>}</td>
                </tr>
              ))}
              {recent.length === 0 && <tr><td colSpan={4} className="muted">{t('reports.noRecent')}</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
