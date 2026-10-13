import { useMemo, useState } from 'react'
import { Api } from '../api/client.js'
import MapView from '../components/MapView.jsx'
import { useNetwork } from '../hooks.js'
import { useI18n } from '../i18n/I18nContext.jsx'
import { STATION_KIND_KEYS, searchStations } from '../utils/text.js'

const EMPTY = { name: '', nameEn: '', aliases: '', stationKind: 'STOP', governorate: 'DAMASCUS', lat: '', lng: '' }

export default function StationsPage() {
  const { t } = useI18n()
  const { stations, refresh } = useNetwork()
  const [form, setForm] = useState(EMPTY)
  const [editing, setEditing] = useState(null)
  const [filter, setFilter] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const list = useMemo(() => (filter ? searchStations(stations, filter, 500) : stations), [stations, filter])
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))
  const edit = (s) => { setEditing(s.id); setForm({ name: s.name, nameEn: s.nameEn || '', aliases: s.aliases || '', stationKind: s.stationKind, governorate: s.governorate, lat: s.location.lat, lng: s.location.lng }) }
  const reset = () => { setEditing(null); setForm(EMPTY); setError('') }

  const save = async (e) => {
    e.preventDefault(); setBusy(true); setError('')
    const body = { name: form.name, nameEn: form.nameEn, aliases: form.aliases, stationKind: form.stationKind, governorate: form.governorate, location: { lat: Number(form.lat), lng: Number(form.lng) } }
    try {
      if (editing) await Api.stations.update(editing, body); else await Api.stations.create(body)
      await refresh(); reset()
    } catch (err) { setError(err.message) } finally { setBusy(false) }
  }
  const remove = async (s) => {
    if (!window.confirm(t('stations.confirmDelete', { name: s.name }))) return
    try { await Api.stations.remove(s.id); await refresh() } catch (err) { setError(err.message) }
  }
  const fit = form.lat && form.lng ? [{ lat: Number(form.lat), lng: Number(form.lng) }] : null

  return (
    <div>
      <h1 style={{ fontSize: '1.4rem' }}>{t('nav.stations')} <span className="muted" style={{ fontSize: '1rem' }}>({stations.length})</span></h1>
      <div className="admin-grid">
        <div>
          <form className="card" onSubmit={save}>
            <h2 style={{ fontSize: '1.1rem' }}>{editing ? t('stations.edit') : t('stations.add')}</h2>
            <div className="row">
              <div className="field" style={{ flex: 1 }}><label>{t('common.name')}</label><input required value={form.name} onChange={set('name')} /></div>
              <div className="field" style={{ flex: 1 }}><label>{t('common.nameEn')}</label><input value={form.nameEn} onChange={set('nameEn')} dir="ltr" /></div>
            </div>
            <div className="field"><label>{t('common.aliases')}</label><input value={form.aliases} onChange={set('aliases')} /></div>
            <div className="row">
              <div className="field" style={{ flex: 1 }}><label>{t('common.kind')}</label>
                <select value={form.stationKind} onChange={set('stationKind')}>{STATION_KIND_KEYS.map((k) => <option key={k} value={k}>{t(`stationKinds.${k}`)}</option>)}</select>
              </div>
              <div className="field" style={{ flex: 1 }}><label>{t('common.governorate')}</label><input value={form.governorate} onChange={set('governorate')} /></div>
            </div>
            <div className="row">
              <div className="field" style={{ flex: 1 }}><label>Latitude</label><input type="number" step="any" required value={form.lat} onChange={set('lat')} /></div>
              <div className="field" style={{ flex: 1 }}><label>Longitude</label><input type="number" step="any" required value={form.lng} onChange={set('lng')} /></div>
            </div>
            <p className="muted" style={{ marginTop: 0 }}>{t('common.coordsHint')}</p>
            {error && <p className="error">{error}</p>}
            <div className="row">
              <button type="submit" disabled={busy}>{editing ? t('common.save') : t('common.add')}</button>
              {editing && <button type="button" className="secondary" onClick={reset}>{t('common.cancel')}</button>}
            </div>
          </form>
          <div className="map"><MapView stations={stations} origin={fit?.[0]} fit={fit} onClick={(p) => setForm((f) => ({ ...f, lat: p.lat, lng: p.lng }))} onStationClick={edit} /></div>
        </div>
        <div>
          <input placeholder={t('common.search')} value={filter} onChange={(e) => setFilter(e.target.value)} style={{ marginBottom: '0.5rem' }} />
          <table>
            <thead><tr><th>{t('common.name')}</th><th>{t('common.kind')}</th><th>{t('common.governorate')}</th><th>{t('common.coords')}</th><th></th></tr></thead>
            <tbody>
              {list.map((s) => (
                <tr key={s.id}>
                  <td><b>{s.name}</b>{s.nameEn && <span className="muted"> · {s.nameEn}</span>}{s.aliases && <div className="muted" style={{ fontSize: '0.8rem' }}>{s.aliases}</div>}</td>
                  <td>{t(`stationKinds.${s.stationKind}`)}</td>
                  <td>{s.governorate}</td>
                  <td dir="ltr" style={{ fontSize: '0.8rem' }}>{s.location.lat}, {s.location.lng}</td>
                  <td className="row">
                    <button className="ghost small" onClick={() => edit(s)}>{t('common.edit')}</button>
                    <button className="ghost small" style={{ color: 'var(--danger)' }} onClick={() => remove(s)}>{t('common.delete')}</button>
                  </td>
                </tr>
              ))}
              {list.length === 0 && <tr><td colSpan={5} className="muted">{t('common.none')}</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
