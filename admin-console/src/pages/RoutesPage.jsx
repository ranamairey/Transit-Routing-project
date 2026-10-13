import { useMemo, useState } from 'react'
import { Api } from '../api/client.js'
import MapView from '../components/MapView.jsx'
import { useNetwork } from '../hooks.js'
import { useI18n } from '../i18n/I18nContext.jsx'
import { ROUTE_STATUS_KEYS, VEHICLE_KEYS } from '../utils/text.js'

const EMPTY = { name: '', nameEn: '', code: '', vehicleType: 'SERVIS', governorate: 'DAMASCUS', currentFare: '', stationIds: [], polyline: [] }

export default function RoutesPage() {
  const { t, lang, formatFare } = useI18n()
  const { stations, routes, refresh } = useNetwork()
  const stationsById = useMemo(() => Object.fromEntries(stations.map((s) => [s.id, s])), [stations])
  const [form, setForm] = useState(EMPTY)
  const [editing, setEditing] = useState(null)
  const [drawMode, setDrawMode] = useState(false)
  const [addStation, setAddStation] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))
  const reset = () => { setEditing(null); setForm(EMPTY); setDrawMode(false); setError('') }
  const edit = (r) => { setEditing(r.id); setForm({ name: r.name, nameEn: r.nameEn || '', code: r.code || '', vehicleType: r.vehicleType, governorate: r.governorate, currentFare: r.currentFare, stationIds: [...r.stationIds], polyline: [...(r.polyline || [])] }) }
  const move = (i, d) => setForm((f) => {
    const ids = [...f.stationIds]; const j = i + d
    if (j < 0 || j >= ids.length) return f
    ;[ids[i], ids[j]] = [ids[j], ids[i]]
    return { ...f, stationIds: ids }
  })
  const pushStation = (id) => { if (id && !form.stationIds.includes(id)) setForm((f) => ({ ...f, stationIds: [...f.stationIds, id] })); setAddStation('') }

  const save = async (e) => {
    e.preventDefault(); setBusy(true); setError('')
    const body = { ...form, code: form.code || null, currentFare: Number(form.currentFare), polyline: form.polyline.length >= 2 ? form.polyline : null }
    try {
      if (editing) await Api.routes.update(editing, body); else await Api.routes.create(body)
      await refresh(); reset()
    } catch (err) { setError(err.message) } finally { setBusy(false) }
  }
  const remove = async (r) => {
    if (!window.confirm(t('routes.confirmDelete', { name: r.name }))) return
    try { await Api.routes.remove(r.id); await refresh() } catch (err) { setError(err.message) }
  }
  const setStatus = async (r, status) => { try { await Api.routes.status(r.id, { status }); await refresh() } catch (err) { setError(err.message) } }
  const setFare = async (r) => {
    const v = window.prompt(t('routes.newFare'), r.currentFare)
    if (v == null || v === '') return
    try { await Api.routes.status(r.id, { currentFare: Number(v) }); await refresh() } catch (err) { setError(err.message) }
  }
  const polyFromStops = () => setForm((f) => ({ ...f, polyline: f.stationIds.map((id) => stationsById[id]?.location).filter(Boolean) }))
  const arrow = lang === 'ar' ? ' ← ' : ' → '

  return (
    <div>
      <h1 style={{ fontSize: '1.4rem' }}>{t('nav.routes')} <span className="muted" style={{ fontSize: '1rem' }}>({routes.length})</span></h1>
      <div className="admin-grid">
        <div>
          <form className="card" onSubmit={save}>
            <h2 style={{ fontSize: '1.1rem' }}>{editing ? t('routes.edit') : t('routes.add')}</h2>
            <div className="row">
              <div className="field" style={{ flex: 1 }}><label>{t('routes.name')}</label><input required value={form.name} onChange={set('name')} placeholder={t('routes.namePlaceholder')} /></div>
              <div className="field" style={{ flex: 1 }}><label>{t('common.nameEn')}</label><input value={form.nameEn} onChange={set('nameEn')} dir="ltr" /></div>
            </div>
            <div className="row">
              <div className="field" style={{ flex: 1 }}><label>{t('routes.code')}</label><input value={form.code} onChange={set('code')} placeholder="R-MEZ-02" dir="ltr" /></div>
              <div className="field" style={{ flex: 1 }}><label>{t('common.kind')}</label>
                <select value={form.vehicleType} onChange={set('vehicleType')}>{VEHICLE_KEYS.map((k) => <option key={k} value={k}>{t(`vehicle.${k}`)}</option>)}</select>
              </div>
            </div>
            <div className="row">
              <div className="field" style={{ flex: 1 }}><label>{t('common.governorate')}</label><input value={form.governorate} onChange={set('governorate')} /></div>
              <div className="field" style={{ flex: 1 }}><label>{t('routes.fare')}</label><input type="number" min="0" step="50" required value={form.currentFare} onChange={set('currentFare')} /></div>
            </div>
            <div className="field">
              <label>{t('routes.stops', { n: form.stationIds.length })}</label>
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
                <option value="">{t('routes.addStop')}</option>
                {stations.filter((s) => !form.stationIds.includes(s.id)).map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </div>
            <div className="field">
              <label>{t('routes.polyline', { n: form.polyline.length })}</label>
              <div className="row">
                <button type="button" className={`small ${drawMode ? '' : 'secondary'}`} onClick={() => setDrawMode(!drawMode)}>{drawMode ? t('routes.drawing') : t('routes.draw')}</button>
                <button type="button" className="secondary small" onClick={polyFromStops}>{t('routes.fromStops')}</button>
                <button type="button" className="secondary small" onClick={() => setForm((f) => ({ ...f, polyline: f.polyline.slice(0, -1) }))}>{t('routes.undo')}</button>
                <button type="button" className="secondary small" onClick={() => setForm((f) => ({ ...f, polyline: [] }))}>{t('routes.clear')}</button>
              </div>
            </div>
            {error && <p className="error">{error}</p>}
            <div className="row">
              <button type="submit" disabled={busy || form.stationIds.length < 2}>{editing ? t('common.save') : t('routes.add')}</button>
              {editing && <button type="button" className="secondary" onClick={reset}>{t('common.cancel')}</button>}
            </div>
          </form>
          <div className="map">
            <MapView stations={stations} routes={editing ? routes.filter((r) => r.id !== editing) : routes} drawing={form.polyline}
              fit={form.polyline.length ? form.polyline : null}
              onClick={(p) => drawMode && setForm((f) => ({ ...f, polyline: [...f.polyline, p] }))}
              onStationClick={(s) => pushStation(s.id)} />
          </div>
        </div>
        <div>
          <table>
            <thead><tr><th>{t('routes.route')}</th><th>{t('common.kind')}</th><th>{t('routes.fare')}</th><th>{t('common.status')}</th><th></th></tr></thead>
            <tbody>
              {routes.map((r) => (
                <tr key={r.id}>
                  <td><b>{r.name}</b>{r.nameEn && <span className="muted"> · {r.nameEn}</span>}<div className="muted" style={{ fontSize: '0.8rem' }}>{r.code} · {r.stops?.map((s) => s.name).join(arrow)}</div></td>
                  <td>{t(`vehicle.${r.vehicleType}`)}</td>
                  <td>{formatFare(r.currentFare)} <button className="ghost small" onClick={() => setFare(r)} title={t('routes.setFare')}>✎</button></td>
                  <td>
                    <select className="small" value={r.status} onChange={(e) => setStatus(r, e.target.value)}>
                      {ROUTE_STATUS_KEYS.map((k) => <option key={k} value={k}>{t(`routeStatus.${k}`)}</option>)}
                    </select>
                  </td>
                  <td className="row">
                    <button className="ghost small" onClick={() => edit(r)}>{t('common.edit')}</button>
                    <button className="ghost small" style={{ color: 'var(--danger)' }} onClick={() => remove(r)}>{t('common.delete')}</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
