import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Api } from '../api/client.js'
import MapView from '../components/MapView.jsx'
import { PageHead, Panel, Stat } from '../components/ui.jsx'
import { useNetwork } from '../hooks.js'
import { useI18n } from '../i18n/I18nContext.jsx'

function Bars({ data, labelFor }) {
  const entries = Object.entries(data || {})
  const max = Math.max(1, ...entries.map(([, v]) => v))
  if (entries.length === 0) return <p className="muted" style={{ margin: 0 }}>—</p>
  return <div className="bars">{entries.map(([k, v]) => (
    <div className="bar" key={k}><span className="label">{labelFor(k)}</span><div className="track"><div className="fill" style={{ width: `${(v / max) * 100}%` }} /></div><b>{v}</b></div>
  ))}</div>
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

  return (
    <div>
      <PageHead eyebrow={t('dash.eyebrow')} title={t('dash.title')} description={t('dash.desc')} />
      {error && <p className="error">{error}</p>}
      <section className="stats">
        <Stat label={t('dash.stations')} value={stats?.stations} detail={t('dash.inNetwork')} icon="♧" />
        <Stat label={t('dash.routes')} value={stats?.routes} detail={t('dash.inNetwork')} icon="≋" tone="mint" />
        <Stat label={t('dash.users')} value={stats?.users} detail={t('dash.registered')} icon="♙" tone="orange" />
        <Stat label={t('dash.reports24')} value={stats?.reportsLast24h} detail={t('reports.last24')} icon="◫" />
        <Stat label={t('dash.pending')} value={stats?.reportsPending} detail={t('dash.needReview')} icon="!" tone="red" />
      </section>
      <div className="two">
        <Panel title={t('dash.byType')} body><Bars data={stats?.reportsByTypeLast24h} labelFor={(k) => t(`reportTypes.${k}`)} /></Panel>
        <Panel title={t('dash.byStatus')} body><Bars data={stats?.routesByStatus} labelFor={(k) => t(`routeStatus.${k}`)} /></Panel>
      </div>
      <div className="two">
        <Panel title={t('dash.map')} body>
          <div className="map-box" style={{ height: 360 }}><MapView stations={stations} routes={routes} /></div>
        </Panel>
        <Panel title={t('dash.recent')} tools={<Link to="/reports" className="button secondary small">{t('dash.viewAll')} →</Link>}>
          <div className="table-wrap">
            <table>
              <thead><tr><th>{t('reports.time')}</th><th>{t('reports.route')}</th><th>{t('reports.type')}</th><th>{t('common.status')}</th></tr></thead>
              <tbody>
                {recent.map((r) => (
                  <tr key={r.id}>
                    <td>{timeAgo(r.sentAt)}</td><td><span className="user-name">{r.routeName}</span></td><td>{t(`reportTypes.${r.reportType}`)}</td>
                    <td>{r.applied ? <span className="badge-role applied">{t('reports.applied')}</span> : <span className="badge-role pending">{t('reports.pendingReview')}</span>}</td>
                  </tr>
                ))}
                {recent.length === 0 && <tr><td colSpan={4} className="empty">{t('reports.noRecent')}</td></tr>}
              </tbody>
            </table>
          </div>
        </Panel>
      </div>
    </div>
  )
}
