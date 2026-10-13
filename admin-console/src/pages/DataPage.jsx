import { useState } from 'react'
import { Api } from '../api/client.js'
import { PageHead, Panel, useToast } from '../components/ui.jsx'
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
  const notify = useToast()
  const [text, setText] = useState('')
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const onFile = async (e) => { const f = e.target.files?.[0]; if (f) setText(await f.text()) }
  const doImport = async () => {
    setBusy(true); setError(''); setResult(null)
    try { setResult(await Api.importNetwork(JSON.parse(text))); notify(t('data.imported')) }
    catch (err) { setError(err.message) } finally { setBusy(false) }
  }
  const doExport = async () => {
    try {
      const snap = await Api.exportNetwork()
      const byId = Object.fromEntries(snap.stations.map((s) => [s.id, s]))
      const out = {
        stations: snap.stations.map((s) => ({ name: s.name, nameEn: s.nameEn, aliases: s.aliases, stationKind: s.stationKind, governorate: s.governorate, lat: s.location.lat, lng: s.location.lng })),
        routes: snap.routes.map((r) => ({ name: r.name, nameEn: r.nameEn, code: r.code, vehicleType: r.vehicleType, governorate: r.governorate, currentFare: r.currentFare, stations: r.stationIds.map((id) => byId[id]?.name).filter(Boolean), polyline: r.polyline })),
      }
      const a = document.createElement('a')
      a.href = URL.createObjectURL(new Blob([JSON.stringify(out, null, 2)], { type: 'application/json' }))
      a.download = `transit-network-${snap.version}.json`; a.click(); URL.revokeObjectURL(a.href)
      notify(t('data.exported'))
    } catch (err) { notify(err.message, 'error') }
  }

  return (
    <div>
      <PageHead eyebrow={t('data.eyebrow')} title={t('data.title')} description={t('data.hint')}
        action={<button className="button secondary" onClick={doExport}>{t('data.export')}</button>} />
      <Panel title={t('data.importTitle')} subtitle={t('data.hint')}
        tools={<>
          <label className="button secondary small" style={{ cursor: 'pointer' }}>📁 {t('data.chooseFile')}<input type="file" accept="application/json" onChange={onFile} style={{ display: 'none' }} /></label>
          <button className="button secondary small" onClick={() => setText(EXAMPLE)}>{t('data.example')}</button>
        </>} body>
        <label className="field-label">{t('data.paste')}</label>
        <textarea className="field code" value={text} onChange={(e) => setText(e.target.value)} placeholder={EXAMPLE} spellCheck={false} />
        {error && <p className="error">{error}</p>}
        <div className="form-actions">
          <button className="button" onClick={doImport} disabled={busy || !text.trim()}>{busy ? t('data.importing') : t('data.importBtn')}</button>
        </div>
        {result && (
          <div style={{ marginTop: 18 }}>
            <p className="ok">{t('data.result', { sc: result.stationsCreated, su: result.stationsUpdated, rc: result.routesCreated, ru: result.routesUpdated })}</p>
            {result.warnings?.length > 0 && <><b>{t('data.warnings')}:</b><ul>{result.warnings.map((w, i) => <li key={i}>{w}</li>)}</ul></>}
          </div>
        )}
      </Panel>
    </div>
  )
}
