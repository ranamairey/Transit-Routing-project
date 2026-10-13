import { useMemo, useState } from 'react'
import { Api } from '../api/client.js'
import MapView from '../components/MapView.jsx'
import { Modal, PageHead, Panel, Stat, useConfirm, useToast } from '../components/ui.jsx'
import { useNetwork } from '../hooks.js'
import { useI18n } from '../i18n/I18nContext.jsx'
import { ROUTE_STATUS_KEYS, VEHICLE_KEYS } from '../utils/text.js'

const EMPTY = { name: '', nameEn: '', code: '', vehicleType: 'SERVIS', governorate: 'DAMASCUS', currentFare: '', stationIds: [], polyline: [] }

export default function RoutesPage() {
  const { t, lang, formatFare } = useI18n()
  const notify = useToast()
  const confirm = useConfirm()
  const { stations, routes, refresh } = useNetwork()
  const stationsById = useMemo(() => Object.fromEntries(stations.map((s) => [s.id, s])), [stations])
  const [editing, setEditing] = useState(null)   // null = closed, '' = new, id = edit
  const [form, setForm] = useState(EMPTY)
  const [drawMode, setDrawMode] = useState(false)
  const [fareModal, setFareModal] = useState(null)
  const [fareValue, setFareValue] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))
  const openNew = () => { setEditing(''); setForm(EMPTY); setDrawMode(false); setError('') }
  const openEdit = (r) => { setEditing(r.id); setForm({ name: r.name, nameEn: r.nameEn || '', code: r.code || '', vehicleType: r.vehicleType, governorate: r.governorate, currentFare: r.currentFare, stationIds: [...r.stationIds], polyline: [...(r.polyline || [])] }); setDrawMode(false); setError('') }
  const close = () => { setEditing(null); setDrawMode(false) }
  const move = (i, d) => setForm((f) => { const ids = [...f.stationIds]; const j = i + d; if (j < 0 || j >= ids.length) return f; [ids[i], ids[j]] = [ids[j], ids[i]]; return { ...f, stationIds: ids } })
  const pushStation = (id) => { if (id && !form.stationIds.includes(id)) setForm((f) => ({ ...f, stationIds: [...f.stationIds, id] })) }
  const polyFromStops = () => setForm((f) => ({ ...f, polyline: f.stationIds.map((id) => stationsById[id]?.location).filter(Boolean) }))

  const save = async (e) => {
    e.preventDefault(); setBusy(true); setError('')
    const body = { ...form, code: form.code || null, currentFare: Number(form.currentFare), polyline: form.polyline.length >= 2 ? form.polyline : null }
    try {
      if (editing) await Api.routes.update(editing, body); else await Api.routes.create(body)
      notify(t('common.saved')); close(); await refresh()
    } catch (err) { setError(err.message) } finally { setBusy(false) }
  }
  const remove = async (r) => {
    if (!(await confirm({ title: t('common.delete'), message: t('routes.confirmDelete', { name: r.name }), danger: true, okLabel: t('common.delete') }))) return
    try { await Api.routes.remove(r.id); notify(t('common.deleted')); await refresh() } catch (err) { notify(err.message, 'error') }
  }
  const setStatus = async (r, status) => { try { await Api.routes.status(r.id, { status }); notify(t('routes.statusUpdated')); await refresh() } catch (err) { notify(err.message, 'error') } }
  const saveFare = async (e) => {
    e.preventDefault()
    try { await Api.routes.status(fareModal.id, { currentFare: Number(fareValue) }); notify(t('routes.fareUpdated')); setFareModal(null); await refresh() } catch (err) { notify(err.message, 'error') }
  }
  const arrow = lang === 'ar' ? ' ← ' : ' → '
  const affected = routes.filter((r) => r.status !== 'NORMAL').length
  const isOpen = editing !== null

  return (
    <div>
      <PageHead eyebrow={t('routes.eyebrow')} title={t('nav.routes')} description={t('routes.desc')}
        action={<button className="button" onClick={openNew}><span style={{ fontSize: 18, lineHeight: 0 }}>＋</span> {t('routes.add')}</button>} />
      <section className="stats">
        <Stat label={t('routes.servis')} value={routes.filter((r) => r.vehicleType === 'SERVIS').length} detail={t('dash.inNetwork')} icon="⌁" />
        <Stat label={t('routes.bus')} value={routes.filter((r) => r.vehicleType === 'BUS').length} detail={t('dash.inNetwork')} icon="≋" tone="mint" />
        <Stat label={t('routes.affected')} value={affected} detail={t('reports.routeStatus')} icon="!" tone={affected ? 'orange' : 'mint'} />
      </section>

      {isOpen ? (
        <div className="two-wide">
          <Panel title={editing ? t('routes.edit') : t('routes.add')} subtitle={drawMode ? t('routes.drawing') : t('stations.mapDesc')} body>
            <div className="map-box" style={{ height: 520 }}>
              <MapView stations={stations} routes={routes.filter((r) => r.id !== editing)} drawing={form.polyline} fit={form.polyline.length ? form.polyline : null}
                onClick={(p) => drawMode && setForm((f) => ({ ...f, polyline: [...f.polyline, p] }))} onStationClick={(s) => pushStation(s.id)} />
            </div>
            <div className="chips" style={{ marginTop: 12 }}>
              <button type="button" className={`chip ${drawMode ? 'on' : ''}`} onClick={() => setDrawMode(!drawMode)}>{t('routes.draw')}</button>
              <button type="button" className="chip" onClick={polyFromStops}>{t('routes.fromStops')}</button>
              <button type="button" className="chip" onClick={() => setForm((f) => ({ ...f, polyline: f.polyline.slice(0, -1) }))}>{t('routes.undo')}</button>
              <button type="button" className="chip" onClick={() => setForm((f) => ({ ...f, polyline: [] }))}>{t('routes.clear')}</button>
              <span className="muted" style={{ alignSelf: 'center', fontSize: 12 }}>{t('routes.polyline', { n: form.polyline.length })}</span>
            </div>
          </Panel>
          <Panel body>
            <form onSubmit={save}>
              <div className="form-grid">
                <div><label className="field-label">{t('routes.name')}</label><input className="field" required autoFocus value={form.name} onChange={set('name')} placeholder={t('routes.namePlaceholder')} /></div>
                <div><label className="field-label">{t('common.nameEn')}</label><input className="field" value={form.nameEn} onChange={set('nameEn')} dir="ltr" /></div>
                <div><label className="field-label">{t('routes.code')}</label><input className="field" value={form.code} onChange={set('code')} placeholder="R-MEZ-02" dir="ltr" /></div>
                <div><label className="field-label">{t('common.kind')}</label><select className="field" value={form.vehicleType} onChange={set('vehicleType')}>{VEHICLE_KEYS.map((k) => <option key={k} value={k}>{t(`vehicle.${k}`)}</option>)}</select></div>
                <div><label className="field-label">{t('common.governorate')}</label><input className="field" value={form.governorate} onChange={set('governorate')} dir="ltr" /></div>
                <div><label className="field-label">{t('routes.fare')}</label><input className="field" type="number" min="0" step="50" required value={form.currentFare} onChange={set('currentFare')} dir="ltr" /></div>
              </div>
              <label className="field-label">{t('routes.stops', { n: form.stationIds.length })}</label>
              <ul className="stops-list">
                {form.stationIds.map((id, i) => (
                  <li key={id}><span className="idx">{i + 1}</span><span>{stationsById[id]?.name || id}</span>
                    <button type="button" className="icon-btn" onClick={() => move(i, -1)}>▲</button>
                    <button type="button" className="icon-btn" onClick={() => move(i, 1)}>▼</button>
                    <button type="button" className="icon-btn delete" onClick={() => setForm((f) => ({ ...f, stationIds: f.stationIds.filter((x) => x !== id) }))}>✕</button></li>
                ))}
              </ul>
              <select className="field" value="" onChange={(e) => pushStation(e.target.value)}>
                <option value="">{t('routes.addStop')}</option>
                {stations.filter((s) => !form.stationIds.includes(s.id)).map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
              {error && <p className="error">{error}</p>}
              <div className="form-actions">
                <button type="submit" className="button" disabled={busy || form.stationIds.length < 2}>{editing ? t('common.save') : t('routes.add')}</button>
                <button type="button" className="button secondary" onClick={close}>{t('common.cancel')}</button>
              </div>
            </form>
          </Panel>
        </div>
      ) : (
        <Panel title={t('routes.all')} count={routes.length} subtitle={t('routes.allDesc')}>
          <div className="table-wrap">
            <table>
              <thead><tr><th>{t('routes.route')}</th><th>{t('common.kind')}</th><th>{t('routes.fare')}</th><th>{t('common.status')}</th><th>{t('common.actions')}</th></tr></thead>
              <tbody>
                {routes.map((r) => (
                  <tr key={r.id}>
                    <td><div className="user-name">{r.name}</div><div className="sub">{[r.nameEn, r.code].filter(Boolean).join(' · ')} · {r.stops?.map((s) => s.name).join(arrow)}</div></td>
                    <td><span className={`badge-role ${r.vehicleType === 'BUS' ? 'rider' : ''}`}>{t(`vehicle.${r.vehicleType}`)}</span></td>
                    <td><span className="user-name">{formatFare(r.currentFare)}</span> <button className="action" onClick={() => { setFareModal(r); setFareValue(r.currentFare) }}>✎</button></td>
                    <td><select className={`filter select-inline`} value={r.status} onChange={(e) => setStatus(r, e.target.value)}>{ROUTE_STATUS_KEYS.map((k) => <option key={k} value={k}>{t(`routeStatus.${k}`)}</option>)}</select></td>
                    <td><div className="actions"><button className="action" onClick={() => openEdit(r)}>{t('common.edit')}</button><button className="action delete" onClick={() => remove(r)}>{t('common.delete')}</button></div></td>
                  </tr>
                ))}
                {routes.length === 0 && <tr><td colSpan={5} className="empty">{t('common.none')}</td></tr>}
              </tbody>
            </table>
          </div>
        </Panel>
      )}

      {fareModal && (
        <Modal title={t('routes.setFare')} subtitle={fareModal.name} onClose={() => setFareModal(null)}>
          <form onSubmit={saveFare}>
            <label className="field-label">{t('routes.newFare')}</label>
            <input className="field" type="number" min="0" step="50" required autoFocus value={fareValue} onChange={(e) => setFareValue(e.target.value)} dir="ltr" />
            <div className="modal-buttons"><button type="button" className="button secondary" onClick={() => setFareModal(null)}>{t('common.cancel')}</button><button type="submit" className="button">{t('common.save')}</button></div>
          </form>
        </Modal>
      )}
    </div>
  )
}
