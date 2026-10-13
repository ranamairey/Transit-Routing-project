import { useMemo, useState } from 'react'
import { Api } from '../../api/client.js'
import MapView from '../../components/MapView.jsx'
import { useI18n } from '../../i18n/I18nContext.jsx'
import { ROUTE_STATUS_KEYS, VEHICLE_KEYS } from '../../utils/text.js'

const NONE = []
const EMPTY = { name: '', code: '', vehicleType: 'SERVIS', governorate: 'DAMASCUS', currentFare: '', stationIds: [], polyline: [] }

/** FR-3.1: routes with ordered stops and a drawn polyline (click the map to add points). */
export default function AdminRoutes({ net }) {
  const { t, lang, formatFare } = useI18n()
  const stations = net.network?.stations || NONE
  const routes = net.network?.routes || NONE
  const stationsById = useMemo(() => Object.fromEntries(stations.map((s) => [s.id, s])), [stations])
  const [form, setForm] = useState(EMPTY)
  const [editing, setEditing] = useState(null)
  const [drawMode, setDrawMode] = useState(false)
  const [addStation, setAddStation] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))
  const reset = () => { setEditing(null); setForm(EMPTY); setDrawMode(false); setError('') }
  const edit = (r) => {
    setEditing(r.id)
    setForm({ name: r.name, code: r.code || '', vehicleType: r.vehicleType, governorate: r.governorate, currentFare: r.currentFare, stationIds: [...r.stationIds], polyline: [...(r.polyline || [])] })
  }
  const move = (i, d) => setForm((f) => {
    const ids = [...f.stationIds]; const j = i + d
    if (j < 0 || j >= ids.length) return f
    ;[ids[i], ids[j]] = [ids[j], ids[i]]
    return { ...f, stationIds: ids }
  })
  const pushStation = (id) => { if (id && !form.stationIds.includes(id)) setForm((f) => ({ ...f, stationIds: [...f.stationIds, id] })); setAddStation('') }

  const save = async (e) => {
    e.preventDefault()
    setBusy(true); setError('')
    const body = { ...form, code: form.code || null, currentFare: Number(form.currentFare), polyline: form.polyline.length >= 2 ? form.polyline : null }
    try {
      if (editing) await Api.admin.updateRoute(editing, body); else await Api.admin.createRoute(body)
      await net.refresh(); reset()
    } catch (err) { setError(err.message) } finally { setBusy(false) }
  }
  const remove = async (r) => {
    if (!window.confirm(t('admin.confirmDeleteRoute', { name: r.name }))) return
    try { await Api.admin.deleteRoute(r.id); await net.refresh() } catch (err) { setError(err.message) }
  }
  const setStatus = async (r, status) => {
    try { await Api.admin.updateRouteStatus(r.id, { status }); await net.refresh() } catch (err) { setError(err.message) }
  }

  const polyFromStops = () => setForm((f) => ({ ...f, polyline: f.stationIds.map((id) => stationsById[id]?.location).filter(Boolean) }))
  const arrow = lang === 'ar' ? ' ← ' : ' → '

  return (
    <div className="admin-grid">
      <div>
        <form className="card" onSubmit={save}>
          <h2 style={{ fontSize: '1.1rem' }}>{editing ? t('admin.editRoute') : t('admin.addRoute')}</h2>
          <div className="field"><label>{t('admin.routeName')}</label><input required value={form.name} onChange={set('name')} placeholder={t('admin.routeNamePlaceholder')} /></div>
          <div className="row">
            <div className="field" style={{ flex: 1 }}><label>{t('admin.code')}</label><input value={form.code} onChange={set('code')} placeholder="R-MEZ-02" /></div>
            <div className="field" style={{ flex: 1 }}><label>{t('admin.kind')}</label>
              <select value={form.vehicleType} onChange={set('vehicleType')}>{VEHICLE_KEYS.map((k) => <option key={k} value={k}>{t(`vehicle.${k}`)}</option>)}</select>
            </div>
          </div>
          <div className="row">
            <div className="field" style={{ flex: 1 }}><label>{t('admin.governorate')}</label><input value={form.governorate} onChange={set('governorate')} /></div>
            <div className="field" style={{ flex: 1 }}><label>{t('admin.fare')}</label><input type="number" min="0" step="50" required value={form.currentFare} onChange={set('currentFare')} /></div>
          </div>
          <div className="field">
            <label>{t('admin.stopsOrdered', { n: form.stationIds.length })}</label>
            <ul className="stops-list">
              {form.stationIds.map((id, i) => (
                <li key={id}>
                  <b>{i + 1}.</b><span>{stationsById[id]?.name || id}</span>
                  <button type="button" className="ghost small" onClick={() => move(i, -1)}>▲</button>
                  <button type="button" className="ghost small" onClick={() => move(i, 1)}>▼</button>
                  <button type="button" className="ghost small" style={{ color: 'var(--danger)' }} onClick={() => setForm((f) => ({ ...f, stationIds: f.stationIds.filter((x) => x !== id) }))}>✕</button>
                </li>
              ))}
            </ul>
            <select value={addStation} onChange={(e) => pushStation(e.target.value)} style={{ marginTop: '0.4rem' }}>
              <option value="">{t('admin.addStop')}</option>
              {stations.filter((s) => !form.stationIds.includes(s.id)).map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </div>
          <div className="field">
            <label>{t('admin.polyline', { n: form.polyline.length })}</label>
            <div className="row">
              <button type="button" className={`small ${drawMode ? '' : 'secondary'}`} onClick={() => setDrawMode(!drawMode)}>{drawMode ? t('admin.drawing') : t('admin.draw')}</button>
              <button type="button" className="secondary small" onClick={polyFromStops}>{t('admin.fromStops')}</button>
              <button type="button" className="secondary small" onClick={() => setForm((f) => ({ ...f, polyline: f.polyline.slice(0, -1) }))}>{t('admin.undo')}</button>
              <button type="button" className="secondary small" onClick={() => setForm((f) => ({ ...f, polyline: [] }))}>{t('admin.clear')}</button>
            </div>
          </div>
          {error && <p className="error">{error}</p>}
          <div className="row">
            <button type="submit" disabled={busy || form.stationIds.length < 2}>{editing ? t('admin.save') : t('admin.addRoute')}</button>
            {editing && <button type="button" className="secondary" onClick={reset}>{t('admin.cancel')}</button>}
          </div>
        </form>
        <div className="map">
          <MapView
            stations={stations}
            routes={editing ? routes.filter((r) => r.id !== editing) : routes}
            drawing={form.polyline}
            fit={form.polyline.length ? form.polyline : null}
            onClick={(p) => drawMode && setForm((f) => ({ ...f, polyline: [...f.polyline, p] }))}
            onStationClick={(s) => pushStation(s.id)}
          />
        </div>
      </div>
      <div>
        <table>
          <thead><tr><th>{t('admin.route')}</th><th>{t('admin.kind')}</th><th>{t('admin.fare')}</th><th>{t('admin.status')}</th><th></th></tr></thead>
          <tbody>
            {routes.map((r) => (
              <tr key={r.id}>
                <td><b>{r.name}</b><div className="muted" style={{ fontSize: '0.8rem' }}>{r.code} · {r.stops?.map((s) => s.name).join(arrow)}</div></td>
                <td>{t(`vehicle.${r.vehicleType}`)}</td>
                <td>{formatFare(r.currentFare)}</td>
                <td>
                  <select className="small" value={r.status} onChange={(e) => setStatus(r, e.target.value)}>
                    {ROUTE_STATUS_KEYS.map((k) => <option key={k} value={k}>{t(`routeStatus.${k}`)}</option>)}
                  </select>
                </td>
                <td className="row">
                  <button className="ghost small" onClick={() => edit(r)}>{t('admin.edit')}</button>
                  <button className="ghost small" style={{ color: 'var(--danger)' }} onClick={() => remove(r)}>{t('admin.delete')}</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
