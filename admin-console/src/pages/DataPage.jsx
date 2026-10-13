import { useState } from 'react'
import { Api } from '../api/client.js'
import { useI18n } from '../i18n/I18nContext.jsx'

const EXAMPLE = `{
  "stations": [
    { "name": "ساحة الأمويين", "nameEn": "Umayyad Square", "aliases": "الأمويين، الأوبرا", "stationKind": "HUB", "governorate": "DAMASCUS", "lat": 33.5138, "lng": 36.2765 }
  ],
  "routes": [
    { "name": "الأمويين - جسر الرئيس", "code": "R-UMA-01", "vehicleType": "SERVIS", "governorate": "DAMASCUS", "currentFare": 800,
      "stations": ["ساحة الأمويين", "جسر الرئيس"], "polyline": [{ "lat": 33.5138, "lng": 36.2765 }, { "lat": 33.511, "lng": 36.289 }] }
  ]
}`

export default function DataPage() {
  const { t } = useI18n()
  const [text, setText] = useState('')
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const onFile = async (e) => {
    const file = e.target.files?.[0]
    if (file) setText(await file.text())
  }
  const doImport = async () => {
    setBusy(true); setError(''); setResult(null)
    try {
      const body = JSON.parse(text)
      setResult(await Api.importNetwork(body))
    } catch (err) { setError(err.message) } finally { setBusy(false) }
  }
  const doExport = async () => {
    try {
      const snap = await Api.exportNetwork()
      // Convert to the import shape so an export can be re-imported directly.
      const stationsById = Object.fromEntries(snap.stations.map((s) => [s.id, s]))
      const out = {
        stations: snap.stations.map((s) => ({ name: s.name, nameEn: s.nameEn, aliases: s.aliases, stationKind: s.stationKind, governorate: s.governorate, lat: s.location.lat, lng: s.location.lng })),
        routes: snap.routes.map((r) => ({ name: r.name, nameEn: r.nameEn, code: r.code, vehicleType: r.vehicleType, governorate: r.governorate, currentFare: r.currentFare, stations: r.stationIds.map((id) => stationsById[id]?.name).filter(Boolean), polyline: r.polyline })),
      }
      const blob = new Blob([JSON.stringify(out, null, 2)], { type: 'application/json' })
      const a = document.createElement('a')
      a.href = URL.createObjectURL(blob); a.download = `transit-network-${snap.version}.json`; a.click()
      URL.revokeObjectURL(a.href)
    } catch (err) { setError(err.message) }
  }

  return (
    <div>
      <h1 style={{ fontSize: '1.4rem' }}>{t('data.title')}</h1>
      <p className="muted">{t('data.hint')}</p>
      <div className="row" style={{ marginBottom: '0.75rem' }}>
        <button className="secondary" onClick={doExport}>{t('data.export')}</button>
        <label className="secondary" style={{ display: 'inline-block', padding: '0.55rem 1rem', borderRadius: 12, background: '#e8ece9', cursor: 'pointer', margin: 0 }}>
          📁 {t('data.chooseFile')}<input type="file" accept="application/json" onChange={onFile} style={{ display: 'none' }} />
        </label>
      </div>
      <div className="field"><label>{t('data.paste')}</label><textarea className="code" rows={14} value={text} onChange={(e) => setText(e.target.value)} placeholder={EXAMPLE} /></div>
      <div className="row">
        <button onClick={doImport} disabled={busy || !text.trim()}>{busy ? t('data.importing') : t('data.importBtn')}</button>
        <button className="ghost" onClick={() => setText(EXAMPLE)}>{t('data.example')}</button>
      </div>
      {error && <p className="error">{error}</p>}
      {result && (
        <div className="card" style={{ marginTop: '0.75rem' }}>
          <p className="ok">{t('data.result', { sc: result.stationsCreated, su: result.stationsUpdated, rc: result.routesCreated, ru: result.routesUpdated })}</p>
          {result.warnings?.length > 0 && <><b>{t('data.warnings')}:</b><ul>{result.warnings.map((w, i) => <li key={i}>{w}</li>)}</ul></>}
        </div>
      )}
    </div>
  )
}
