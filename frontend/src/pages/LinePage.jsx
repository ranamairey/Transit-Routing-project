import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Api } from '../api/client.js'
import LiveBadge from '../components/LiveBadge.jsx'
import MapView from '../components/MapView.jsx'
import ReportDialog from '../components/ReportDialog.jsx'
import SuggestDialog from '../components/SuggestDialog.jsx'
import { useToast } from '../components/Toast.jsx'
import { useRider } from '../context/RiderContext.jsx'
import { useI18n } from '../i18n/I18nContext.jsx'
import { altNameOf, nameOf } from '../utils/names.js'

/** One line: map, ordered stops, fare, live status and the latest rider reports; follow it for alerts. */
export default function LinePage({ net }) {
  const { id } = useParams()
  const { t, lang, dark, formatFare, timeAgo } = useI18n()
  const notify = useToast()
  const { live, isFollowing, toggleFollow } = useRider()
  const routes = net.network?.routes || []
  const stations = net.network?.stations || []
  const route = routes.find((r) => r.id === id)
  const byId = useMemo(() => Object.fromEntries(stations.map((s) => [s.id, s])), [stations])
  const stops = useMemo(() => (route?.stationIds || []).map((sid) => byId[sid]).filter(Boolean), [route, byId])
  const fit = useMemo(() => (route ? (route.polyline?.length ? route.polyline : stops.map((s) => s.location)) : null), [route, stops])
  const [reports, setReports] = useState([])
  const [dialog, setDialog] = useState(null)

  useEffect(() => {
    if (!id) return
    Api.recentReports(id).then(setReports).catch(() => setReports([]))
  }, [id])

  if (!route) return <main className="page"><p className="muted">{net.loading ? t('planner.loadingNetwork') : t('lines.notFound')}</p><Link to="/lines">{t('lines.back')}</Link></main>

  const follow = async () => notify((await toggleFollow(route.id)) ? t('lines.followed') : t('lines.unfollowed'))

  return (
    <main className="workspace">
      <aside className="sidebar">
        <section className="panel">
          <Link to="/lines" className="back-link">{t('lines.back')}</Link>
          <div className="panel-head" style={{ marginTop: 8 }}>
            <div>
              <h2>{nameOf(route, lang)}</h2>
              {altNameOf(route, lang) && <p className="sub">{altNameOf(route, lang)}</p>}
            </div>
            <span className={`veh ${route.vehicleType}`}>{route.vehicleType === 'BUS' ? '🚌' : '🚍'}</span>
          </div>
          <dl className="facts">
            {route.code && <><dt>#</dt><dd>{route.code}</dd></>}
            <dt>{t('lines.vehicle')}</dt><dd>{t(`vehicle.${route.vehicleType}`)}</dd>
            <dt>{t('lines.fare')}</dt><dd>{formatFare(route.currentFare)}</dd>
            <dt>{t('lines.status')}</dt><dd>{live[route.id] ? <LiveBadge live={live[route.id]} /> : t('routeStatus.NORMAL')}</dd>
          </dl>
          <div className="row wrap">
            <button className={`btn ${isFollowing(route.id) ? '' : 'secondary'}`} onClick={follow}>{isFollowing(route.id) ? t('lines.unfollow') : t('lines.follow')}</button>
            <button className="btn secondary" onClick={() => setDialog('report')}>{t('lines.report')}</button>
            <button className="btn secondary" onClick={() => setDialog('suggest')}>{t('lines.suggest')}</button>
          </div>
        </section>

        <section className="panel">
          <h3 className="section-title">{t('lines.reportsTitle')}</h3>
          {reports.length === 0 ? <p className="sub">{t('lines.noReports')}</p> : (
            <ul className="report-list">
              {reports.slice(0, 10).map((r) => (
                <li key={r.id}>
                  <b>{t(`reportTypes.${r.reportType}`)}</b>
                  {r.newFare != null && <> — {formatFare(r.newFare)}</>}
                  {r.stationName && <> · {r.stationName}</>}
                  {r.description && <p>{r.description}</p>}
                  <small>{timeAgo(r.sentAt)}</small>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="panel">
          <h3 className="section-title">{t('lines.stopsTitle')} <span className="tag">{stops.length}</span></h3>
          <ol className="stop-list">
            {stops.map((s) => (
              <li key={s.id}><Link to={`/stops/${s.id}`}>{nameOf(s, lang)}</Link>{altNameOf(s, lang) && <small>{altNameOf(s, lang)}</small>}</li>
            ))}
          </ol>
        </section>
      </aside>
      <section className="mapcard">
        <MapView dark={dark} routes={[route]} stations={stops} fit={fit} highlightRouteId={route.id} zoomPosition="bottomright" />
      </section>
      {dialog === 'report' && <ReportDialog leg={{ routeId: route.id, routeName: route.name }} stations={stops} onClose={() => setDialog(null)} />}
      {dialog === 'suggest' && <SuggestDialog initial={{ type: 'WRONG_LINE', routeId: route.id }} stations={stations} routes={routes} onClose={() => setDialog(null)} />}
    </main>
  )
}
