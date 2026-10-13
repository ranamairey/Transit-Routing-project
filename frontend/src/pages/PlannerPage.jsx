import { useMemo, useState } from 'react'
import { Api } from '../api/client.js'
import AlertsBar from '../components/AlertsBar.jsx'
import EndpointPicker from '../components/EndpointPicker.jsx'
import MapView, { legPath } from '../components/MapView.jsx'
import ReportDialog from '../components/ReportDialog.jsx'
import TripOptions from '../components/TripOptions.jsx'
import { useI18n } from '../i18n/I18nContext.jsx'

const LAST_KEY = 'sst.lastSearch'
const EMPTY = []

export default function PlannerPage({ net }) {
  const { network, offline, loading } = net
  const { t, lang } = useI18n()
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
  const [reportLeg, setReportLeg] = useState(null)

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
      setResult(data); setSelected(0)
      try { localStorage.setItem(LAST_KEY, JSON.stringify(data)) } catch { /* ignore */ }
    } catch (err) {
      setError(offline ? t('planner.offlineSearch') : err.message)
    } finally {
      setBusy(false)
    }
  }

  const path = result?.paths?.[selected]
  const legs = useMemo(() => (path ? path.legs.map((l) => ({ ...l, path: legPath(l, routesById) })) : []), [path, routesById])
  const fit = useMemo(() => {
    if (legs.length) return legs.flatMap((l) => l.path)
    const pts = [origin?.point, dest?.point].filter(Boolean)
    return pts.length ? pts : null
  }, [legs, origin, dest])

  // The backend labels free points in Arabic; show them in the current language.
  const endpointName = (name, key) => (name === 'نقطة الانطلاق' || name === 'الوجهة' ? t(key) : name)

  return (
    <div className="planner">
      <aside className="side">
        <h1 style={{ fontSize: '1.3rem' }}>{t('planner.title')}</h1>
        <p className="muted" style={{ marginTop: 0 }}>{t('planner.subtitle')}</p>
        {loading && !network && <p>{t('planner.loadingNetwork')}</p>}
        <EndpointPicker label={t('planner.from')} kind="origin" value={origin} onChange={setOrigin} stations={stations} pinMode={pinMode === 'origin'} onPinMode={setPinMode} />
        <EndpointPicker label={t('planner.to')} kind="dest" value={dest} onChange={setDest} stations={stations} pinMode={pinMode === 'dest'} onPinMode={setPinMode} />
        <div className="row between" style={{ marginBottom: '0.75rem' }}>
          <label style={{ margin: 0 }}>{t('planner.peak')}</label>
          <select style={{ width: 'auto' }} value={peak} onChange={(e) => setPeak(e.target.value)}>
            <option value="auto">{t('planner.peakAuto')}</option>
            <option value="yes">{t('planner.yes')}</option>
            <option value="no">{t('planner.no')}</option>
          </select>
        </div>
        <button style={{ width: '100%' }} onClick={search} disabled={!origin || !dest || busy}>{busy ? t('planner.searching') : t('planner.search')}</button>
        {error && <p className="error">{error}</p>}
        {result && (
          <div style={{ marginTop: '1rem' }}>
            <p className="muted" style={{ fontSize: '0.85rem' }}>
              {endpointName(result.originName, 'planner.origin')} {lang === 'ar' ? '←' : '→'} {endpointName(result.destinationName, 'planner.destination')} · {t('planner.computedIn', { ms: result.processingMillis })}
            </p>
            <AlertsBar alerts={result.alerts} />
            <TripOptions paths={result.paths} selected={selected} onSelect={setSelected} onReport={setReportLeg} peak={result.peak} />
          </div>
        )}
      </aside>
      <main className="map">
        <MapView
          stations={stations}
          routes={routes}
          origin={origin?.point}
          destination={dest?.point}
          legs={legs}
          fit={fit}
          onClick={onMapClick}
          onStationClick={onStationClick}
        />
      </main>
      {reportLeg && <ReportDialog leg={reportLeg} stations={stations.filter((s) => routesById[reportLeg.routeId]?.stationIds?.includes(s.id))} onClose={() => setReportLeg(null)} />}
    </div>
  )
}
