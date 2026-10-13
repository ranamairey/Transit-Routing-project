import { useMemo, useState } from 'react'
import { Api } from '../api/client.js'
import AlertsBar from '../components/AlertsBar.jsx'
import EndpointPicker from '../components/EndpointPicker.jsx'
import MapView, { legPath } from '../components/MapView.jsx'
import ReportDialog from '../components/ReportDialog.jsx'
import Select from '../components/Select.jsx'
import { useToast } from '../components/Toast.jsx'
import TripOptions from '../components/TripOptions.jsx'
import { useI18n } from '../i18n/I18nContext.jsx'

const LAST_KEY = 'sst.lastSearch'
const EMPTY = []
const NEAR_METERS = 800

const distMeters = (a, b) => {
  const dLat = (b.lat - a.lat) * 111320
  const dLng = (b.lng - a.lng) * 111320 * Math.cos((a.lat * Math.PI) / 180)
  return Math.sqrt(dLat * dLat + dLng * dLng)
}

export default function PlannerPage({ net }) {
  const { network, offline, loading } = net
  const { t, formatFare, formatDuration } = useI18n()
  const notify = useToast()
  const stations = network?.stations || EMPTY
  const routes = network?.routes || EMPTY
  const routesById = useMemo(() => Object.fromEntries(routes.map((r) => [r.id, r])), [routes])

  const [origin, setOrigin] = useState(null)
  const [dest, setDest] = useState(null)
  const [pinMode, setPinMode] = useState(null)
  const [peak, setPeak] = useState('auto')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [result, setResult] = useState(() => {
    try { return JSON.parse(localStorage.getItem(LAST_KEY) || 'null') } catch { return null }
  })
  const [selected, setSelected] = useState(0)
  const [showRoute, setShowRoute] = useState(true)
  const [showAll, setShowAll] = useState(false)
  const [fitKey, setFitKey] = useState(0)
  const [reportLeg, setReportLeg] = useState(null)

  const setPin = (mode) => {
    setPinMode(mode)
    if (mode) notify(t('planner.pickOnMap', { what: t(mode === 'origin' ? 'planner.origin' : 'planner.destination') }))
  }
  const onMapClick = (point) => {
    if (!pinMode) return
    const value = { mode: 'pin', name: `${t('planner.pin')} (${point.lat.toFixed(4)}, ${point.lng.toFixed(4)})`, point }
    if (pinMode === 'origin') setOrigin(value); else setDest(value)
    setPinMode(null)
  }
  const onStationClick = (s) => {
    const value = { mode: 'station', name: s.name, stationId: s.id, point: s.location }
    if (pinMode === 'dest' || (!pinMode && origin && !dest)) setDest(value)
    else if (pinMode === 'origin' || !origin) setOrigin(value)
    setPinMode(null)
  }

  const search = async () => {
    if (!origin || !dest) return
    setBusy(true); setError('')
    const request = {
      origin: origin.mode === 'station' ? null : origin.point,
      destination: dest.mode === 'station' ? null : dest.point,
      originStationId: origin.mode === 'station' ? origin.stationId : null,
      destinationStationId: dest.mode === 'station' ? dest.stationId : null,
      peak: peak === 'auto' ? null : peak === 'yes',
    }
    try {
      const data = await Api.routing(request)
      setResult(data); setSelected(0); setShowRoute(true); setFitKey((k) => k + 1)
      notify(t('planner.found', { n: data.paths.length }))
      try { localStorage.setItem(LAST_KEY, JSON.stringify(data)) } catch { /* ignore */ }
    } catch (err) {
      setError(offline ? t('planner.offlineSearch') : err.message)
    } finally {
      setBusy(false)
    }
  }

  const share = async (p) => {
    const text = t('planner.shareText', {
      from: endpointName(result.originName, 'planner.origin'), to: endpointName(result.destinationName, 'planner.destination'),
      fare: formatFare(p.totalFare), dur: formatDuration(p.totalDurationSeconds),
    })
    try {
      if (navigator.share) await navigator.share({ text })
      else if (navigator.clipboard) { await navigator.clipboard.writeText(text); notify(t('planner.shared')) }
      else notify(text)
    } catch { /* cancelled */ }
  }

  const path = result?.paths?.[selected]

  // Only draw the lines that matter: the chosen trip's lines, else lines serving the picked points, else the whole network faintly.
  const { visibleRoutes, dimRoutes } = useMemo(() => {
    if (showAll) return { visibleRoutes: routes, dimRoutes: false }
    if (path && showRoute) {
      const ids = new Set(path.legs.map((l) => l.routeId).filter(Boolean))
      return { visibleRoutes: routes.filter((r) => ids.has(r.id)), dimRoutes: false }
    }
    const points = [origin?.point, dest?.point].filter(Boolean)
    if (points.length === 0) return { visibleRoutes: routes, dimRoutes: true }
    const nearIds = new Set(stations.filter((s) => points.some((p) => distMeters(p, s.location) <= NEAR_METERS)).map((s) => s.id))
    return { visibleRoutes: routes.filter((r) => r.stationIds?.some((id) => nearIds.has(id))), dimRoutes: false }
  }, [showAll, path, showRoute, routes, stations, origin, dest])
  const visibleStations = useMemo(() => {
    if (showAll || dimRoutes) return stations
    const ids = new Set(visibleRoutes.flatMap((r) => r.stationIds || []))
    return stations.filter((s) => ids.has(s.id))
  }, [showAll, dimRoutes, visibleRoutes, stations])
  const legs = useMemo(() => (path && showRoute ? path.legs.map((l) => ({ ...l, path: legPath(l, routesById) })) : []), [path, routesById, showRoute])
  const fit = useMemo(() => {
    if (legs.length) return legs.flatMap((l) => l.path)
    const pts = [origin?.point, dest?.point].filter(Boolean)
    return pts.length ? pts : null
  }, [legs, origin, dest, fitKey]) // eslint-disable-line react-hooks/exhaustive-deps
  const endpointName = (name, key) => (name === 'نقطة الانطلاق' || name === 'الوجهة' ? t(key) : name)

  return (
    <main className="workspace">
      <aside className="sidebar">
        <section className="panel">
          <div className="panel-head">
            <div><h2>{t('planner.title')} <span className="spark">✦</span></h2><p className="sub">{t('planner.subtitle')}</p></div>
            <span className="tag">{t('planner.city')}</span>
          </div>
          <div className="section-title">{t('planner.details')} <span className="step-no">{t('planner.step')}</span></div>
          {loading && !network && <p className="sub">{t('planner.loadingNetwork')}</p>}
          <div className="inputs">
            <EndpointPicker label={t('planner.origin')} kind="origin" value={origin} onChange={setOrigin} stations={stations} pinMode={pinMode === 'origin'} onPinMode={setPin} />
            <EndpointPicker label={t('planner.destination')} kind="dest" value={dest} onChange={setDest} stations={stations} pinMode={pinMode === 'dest'} onPinMode={setPin} />
          </div>
          <div className="travel-options">
            <label htmlFor="peak">{t('planner.departure')}</label>
            <Select ariaLabel={t('planner.departure')} value={peak} onChange={setPeak} style={{ maxWidth: 235 }}
              options={[{ value: 'auto', label: t('planner.peakAuto') }, { value: 'yes', label: t('planner.yes') }, { value: 'no', label: t('planner.no') }]} />
          </div>
          <button className="primary" onClick={search} disabled={!origin || !dest || busy}>{busy ? t('planner.searching') : t('planner.search')}</button>
          {error ? <p className="error" style={{ textAlign: 'center', marginBottom: 0 }}>{error}</p> : <p className="hint">{t('planner.hintSearch')}</p>}
        </section>

        <AlertsBar alerts={result?.alerts} />

        {result && (
          <TripOptions paths={result.paths} selected={selected} peak={result.peak}
            onSelect={(i) => { setSelected(i); setShowRoute(true) }}
            onReport={setReportLeg}
            onShow={(i) => { setSelected(i); setShowRoute(true); setFitKey((k) => k + 1); notify(t('planner.routeShown')) }}
            onShare={share} />
        )}
      </aside>

      <section className="mapcard" aria-label={t('planner.mapTitle')}>
        <MapView stations={visibleStations} routes={visibleRoutes} dimRoutes={dimRoutes} origin={origin?.point} destination={dest?.point} legs={legs} fit={fit}
          onClick={onMapClick} onStationClick={onStationClick} zoomPosition="bottomright" />
        <div className="map-heading"><b>{t('planner.mapTitle')}</b><span>{t('planner.mapSub')}</span></div>
        {pinMode && <div className="map-pick">📍 {t('planner.pickOnMap', { what: t(pinMode === 'origin' ? 'planner.origin' : 'planner.destination') })}</div>}
        <div className="map-tools">
          <button onClick={() => setFitKey((k) => k + 1)}>◎ {t('planner.recenter')}</button>
          <button onClick={() => setShowAll(!showAll)}>{showAll ? `◫ ${t('planner.hideAllLines')}` : `◫ ${t('planner.showAllLines')}`}</button>
          {path && <button onClick={() => setShowRoute(!showRoute)}>{showRoute ? `× ${t('planner.clearRoute')}` : `↗ ${t('planner.showRoute')}`}</button>}
        </div>
        <div className={`map-badge ${offline ? 'off' : ''}`}><span />{offline ? t('planner.cached') : t('planner.live')}</div>
      </section>

      {reportLeg && <ReportDialog leg={reportLeg} stations={stations.filter((s) => routesById[reportLeg.routeId]?.stationIds?.includes(s.id))} onClose={() => setReportLeg(null)} />}
    </main>
  )
}
