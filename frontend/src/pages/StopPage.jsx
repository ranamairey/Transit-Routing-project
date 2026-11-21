import { useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import LiveBadge from '../components/LiveBadge.jsx'
import MapView from '../components/MapView.jsx'
import SuggestDialog from '../components/SuggestDialog.jsx'
import { useToast } from '../components/Toast.jsx'
import { useRider } from '../context/RiderContext.jsx'
import { useI18n } from '../i18n/I18nContext.jsx'
import { encodeEndpoint } from '../utils/endpoints.js'
import { altNameOf, nameOf } from '../utils/names.js'

/** A stop or place: every line that stops there, plus shortcuts to plan a trip from or to it. */
export default function StopPage({ net }) {
  const { id } = useParams()
  const navigate = useNavigate()
  const notify = useToast()
  const { t, lang, dark, formatFare } = useI18n()
  const { live, addPlace } = useRider()
  const routes = net.network?.routes || []
  const stations = net.network?.stations || []
  const station = stations.find((s) => s.id === id)
  const lines = useMemo(() => routes.filter((r) => r.stationIds?.includes(id)), [routes, id])
  const fit = useMemo(() => (station ? [station.location] : null), [station])
  const [suggest, setSuggest] = useState(false)

  if (!station) return <main className="page"><p className="muted">{net.loading ? t('planner.loadingNetwork') : t('stop.notFound')}</p><Link to="/lines">{t('lines.back')}</Link></main>

  const endpoint = { mode: 'station', name: station.name, stationId: station.id, point: station.location }
  const plan = (side) => navigate(`/?${side}=${encodeURIComponent(encodeEndpoint(endpoint))}`)

  return (
    <main className="workspace">
      <aside className="sidebar">
        <section className="panel">
          <button className="back-link" onClick={() => navigate(-1)}>{t('stop.back')}</button>
          <h2 style={{ marginTop: 8 }}>{nameOf(station, lang)}</h2>
          {altNameOf(station, lang) && <p className="sub">{altNameOf(station, lang)}</p>}
          {station.aliases && <p className="sub">{station.aliases}</p>}
          <p className="sub">{t(`stationKinds.${station.stationKind}`)}</p>
          <div className="row wrap" style={{ marginTop: 12 }}>
            <button className="btn" onClick={() => plan('from')}>{t('lines.planFrom')}</button>
            <button className="btn" onClick={() => plan('to')}>{t('lines.planTo')}</button>
            <button className="btn secondary" onClick={() => { addPlace(endpoint, 'OTHER', nameOf(station, lang)); notify(t('places.saved')) }}>{t('stop.save')}</button>
            <button className="btn secondary" onClick={() => setSuggest(true)}>{t('stop.wrongPlace')}</button>
          </div>
        </section>
        <section className="panel">
          <h3 className="section-title">{t('stop.lines')} <span className="tag">{lines.length}</span></h3>
          {lines.length === 0 ? <p className="sub">{t('stop.noLines')}</p> : (
            <ul className="stop-lines">
              {lines.map((r) => (
                <li key={r.id}>
                  <Link to={`/lines/${r.id}`}>
                    <span className={`veh small ${r.vehicleType}`}>{r.vehicleType === 'BUS' ? '🚌' : '🚍'}</span>
                    <span><b>{nameOf(r, lang)}</b><small>{r.code && <>{r.code} · </>}{formatFare(r.currentFare)}</small></span>
                  </Link>
                  <LiveBadge live={live[r.id]} />
                </li>
              ))}
            </ul>
          )}
        </section>
      </aside>
      <section className="mapcard">
        <MapView dark={dark} routes={lines} stations={[station]} fit={fit} zoomPosition="bottomright" />
      </section>
      {suggest && <SuggestDialog initial={{ type: 'WRONG_LOCATION', stationId: station.id }} stations={stations} routes={routes} onClose={() => setSuggest(false)} />}
    </main>
  )
}
