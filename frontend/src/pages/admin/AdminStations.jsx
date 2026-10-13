import { useMemo, useState } from 'react'
import { Api } from '../../api/client.js'
import MapView from '../../components/MapView.jsx'
import { useI18n } from '../../i18n/I18nContext.jsx'
import { STATION_KIND_KEYS, searchStations } from '../../utils/text.js'

const NONE = []
const EMPTY = { name: '', aliases: '', stationKind: 'STOP', governorate: 'DAMASCUS', lat: '', lng: '' }

/** FR-3.1: stations with precise coordinates (click the map to set them). */
export default function AdminStations({ net }) {
  const { t } = useI18n()
  const stations = net.network?.stations || NONE
  const [form, setForm] = useState(EMPTY)
  const [editing, setEditing] = useState(null)
  const [filter, setFilter] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const list = useMemo(() => (filter ? searchStations(stations, filter, 100) : stations), [stations, filter])
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  const edit = (s) => {
    setEditing(s.id)
    setForm({ name: s.name, aliases: s.aliases || '', stationKind: s.stationKind, governorate: s.governorate, lat: s.location.lat, lng: s.location.lng })
  }
  const reset = () => { setEditing(null); setForm(EMPTY); setError('') }

  const save = async (e) => {
    e.preventDefault()
    setBusy(true); setError('')
    const body = { name: form.name, aliases: form.aliases, stationKind: form.stationKind, governorate: form.governorate, location: { lat: Number(form.lat), lng: Number(form.lng) } }
    try {
      if (editing) await Api.admin.updateStation(editing, body); else await Api.admin.createStation(body)
      await net.refresh(); reset()
    } catch (err) { setError(err.message) } finally { setBusy(false) }
  }
  const remove = async (s) => {
    if (!window.confirm(t('admin.confirmDeleteStation', { name: s.name }))) return
    try { await Api.admin.deleteStation(s.id); await net.refresh() } catch (err) { setError(err.message) }
  }

  const fit = form.lat && form.lng ? [{ lat: Number(form.lat), lng: Number(form.lng) }] : null

  return (
    <div className="admin-grid">
      <div>
        <form className="card" onSubmit={save}>
          <h2 style={{ fontSize: '1.1rem' }}>{editing ? t('admin.editStation') : t('admin.addStation')}</h2>
          <div className="field"><label>{t('admin.name')}</label><input required value={form.name} onChange={set('name')} /></div>
          <div className="field"><label>{t('admin.aliases')}</label><input value={form.aliases} onChange={set('aliases')} placeholder={t('admin.aliasesPlaceholder')} /></div>
          <div className="row">
            <div className="field" style={{ flex: 1 }}><label>{t('admin.kind')}</label>
              <select value={form.stationKind} onChange={set('stationKind')}>{STATION_KIND_KEYS.map((k) => <option key={k} value={k}>{t(`stationKinds.${k}`)}</option>)}</select>
            </div>
            <div className="field" style={{ flex: 1 }}><label>{t('admin.governorate')}</label><input value={form.governorate} onChange={set('governorate')} /></div>
          </div>
          <div className="row">
            <div className="field" style={{ flex: 1 }}><label>Latitude</label><input type="number" step="any" required value={form.lat} onChange={set('lat')} /></div>
            <div className="field" style={{ flex: 1 }}><label>Longitude</label><input type="number" step="any" required value={form.lng} onChange={set('lng')} /></div>
          </div>
          <p className="muted" style={{ marginTop: 0 }}>{t('admin.coordsHint')}</p>
          {error && <p className="error">{error}</p>}
          <div className="row">
            <button type="submit" disabled={busy}>{editing ? t('admin.save') : t('admin.add')}</button>
            {editing && <button type="button" className="secondary" onClick={reset}>{t('admin.cancel')}</button>}
          </div>
        </form>
        <div className="map"><MapView stations={stations} origin={fit?.[0]} fit={fit} onClick={(p) => setForm((f) => ({ ...f, lat: p.lat, lng: p.lng }))} onStationClick={edit} /></div>
      </div>
      <div>
        <input placeholder={t('admin.searchStations')} value={filter} onChange={(e) => setFilter(e.target.value)} style={{ marginBottom: '0.5rem' }} />
        <table>
          <thead><tr><th>{t('admin.name')}</th><th>{t('admin.kind')}</th><th>{t('admin.governorate')}</th><th>{t('admin.coords')}</th><th></th></tr></thead>
          <tbody>
            {list.map((s) => (
              <tr key={s.id}>
                <td><b>{s.name}</b>{s.aliases && <div className="muted" style={{ fontSize: '0.8rem' }}>{s.aliases}</div>}</td>
                <td>{t(`stationKinds.${s.stationKind}`)}</td>
                <td>{s.governorate}</td>
                <td dir="ltr" style={{ fontSize: '0.8rem' }}>{s.location.lat}, {s.location.lng}</td>
                <td className="row">
                  <button className="ghost small" onClick={() => edit(s)}>{t('admin.edit')}</button>
                  <button className="ghost small" style={{ color: 'var(--danger)' }} onClick={() => remove(s)}>{t('admin.delete')}</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
