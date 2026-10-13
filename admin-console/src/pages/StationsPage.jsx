import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Api } from '../api/client.js'
import MapView from '../components/MapView.jsx'
import Select from '../components/Select.jsx'
import { Modal, PageHead, Panel, SearchBox, Stat, useConfirm, useToast } from '../components/ui.jsx'
import { useNetwork } from '../hooks.js'
import { useI18n } from '../i18n/I18nContext.jsx'
import { STATION_KIND_KEYS, searchStations } from '../utils/text.js'

const EMPTY = { name: '', nameEn: '', aliases: '', stationKind: 'STOP', governorate: 'DAMASCUS', lat: '', lng: '' }

export default function StationsPage() {
  const { t } = useI18n()
  const notify = useToast()
  const confirm = useConfirm()
  const { stations, routes, refresh } = useNetwork()
  const [params] = useSearchParams()
  const [filter, setFilter] = useState(params.get('q') || '')
  const [modal, setModal] = useState(null) // { id } | { id: null }
  const [form, setForm] = useState(EMPTY)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  useEffect(() => { setFilter(params.get('q') || '') }, [params])

  const list = useMemo(() => (filter ? searchStations(stations, filter, 500) : stations), [stations, filter])
  const hubs = stations.filter((s) => s.stationKind !== 'STOP' && s.stationKind !== 'POINT').length
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))
  const openNew = (point) => { setModal({ id: null }); setForm({ ...EMPTY, lat: point?.lat ?? '', lng: point?.lng ?? '' }); setError('') }
  const openEdit = (s) => { setModal({ id: s.id }); setForm({ name: s.name, nameEn: s.nameEn || '', aliases: s.aliases || '', stationKind: s.stationKind, governorate: s.governorate, lat: s.location.lat, lng: s.location.lng }); setError('') }
  const close = () => setModal(null)

  const save = async (e) => {
    e.preventDefault(); setBusy(true); setError('')
    const body = { name: form.name, nameEn: form.nameEn, aliases: form.aliases, stationKind: form.stationKind, governorate: form.governorate, location: { lat: Number(form.lat), lng: Number(form.lng) } }
    try {
      if (modal.id) await Api.stations.update(modal.id, body); else await Api.stations.create(body)
      notify(t('common.saved')); close(); await refresh()
    } catch (err) { setError(err.message) } finally { setBusy(false) }
  }
  const remove = async (s) => {
    if (!(await confirm({ title: t('common.delete'), message: t('stations.confirmDelete', { name: s.name }), danger: true, okLabel: t('common.delete') }))) return
    try { await Api.stations.remove(s.id); notify(t('common.deleted')); await refresh() } catch (err) { notify(err.message, 'error') }
  }
  const fit = modal && form.lat && form.lng ? [{ lat: Number(form.lat), lng: Number(form.lng) }] : null

  return (
    <div>
      <PageHead eyebrow={t('stations.eyebrow')} title={t('nav.stations')} description={t('stations.desc')}
        action={<button className="button" onClick={() => openNew()}><span style={{ fontSize: 18, lineHeight: 0 }}>＋</span> {t('stations.add')}</button>} />
      <section className="stats">
        <Stat label={t('nav.stations')} value={stations.length} detail={t('dash.inNetwork')} icon="♧" />
        <Stat label={t('stations.hubs')} value={hubs} detail={t('stations.byKind')} icon="⌁" tone="mint" />
        <Stat label={t('nav.routes')} value={routes.length} detail={t('dash.inNetwork')} icon="≋" tone="orange" />
      </section>
      <div className="two-wide">
        <Panel title={t('stations.mapTitle')} subtitle={t('stations.mapDesc')} body>
          <div className="map-box"><MapView stations={stations} routes={routes} origin={fit?.[0]} fit={fit} onClick={(p) => (modal ? setForm((f) => ({ ...f, lat: p.lat, lng: p.lng })) : openNew(p))} onStationClick={openEdit} /></div>
        </Panel>
        <Panel title={t('stations.all')} count={list.length} subtitle={t('stations.allDesc')}
          tools={<SearchBox value={filter} onChange={setFilter} placeholder={t('common.search')} />}>
          <div className="table-wrap">
            <table>
              <thead><tr><th>{t('common.name')}</th><th>{t('common.kind')}</th><th>{t('common.governorate')}</th><th>{t('common.coords')}</th><th>{t('common.actions')}</th></tr></thead>
              <tbody>
                {list.map((s) => (
                  <tr key={s.id}>
                    <td><div className="user-name">{s.name}</div><div className="sub">{[s.nameEn, s.aliases].filter(Boolean).join(' · ')}</div></td>
                    <td><span className={`badge-role ${s.stationKind === 'STOP' || s.stationKind === 'POINT' ? 'rider' : ''}`}>{t(`stationKinds.${s.stationKind}`)}</span></td>
                    <td>{s.governorate}</td>
                    <td dir="ltr" style={{ textAlign: 'start', fontSize: 11 }}>{s.location.lat}, {s.location.lng}</td>
                    <td><div className="actions"><button className="action" onClick={() => openEdit(s)}>{t('common.edit')}</button><button className="action delete" onClick={() => remove(s)}>{t('common.delete')}</button></div></td>
                  </tr>
                ))}
                {list.length === 0 && <tr><td colSpan={5} className="empty">{t('common.none')}</td></tr>}
              </tbody>
            </table>
          </div>
        </Panel>
      </div>

      {modal && (
        <Modal title={modal.id ? t('stations.edit') : t('stations.add')} subtitle={t('common.coordsHint')} onClose={close}>
          <form onSubmit={save}>
            <div className="form-grid">
              <div><label className="field-label">{t('common.name')}</label><input className="field" required autoFocus value={form.name} onChange={set('name')} /></div>
              <div><label className="field-label">{t('common.nameEn')}</label><input className="field" value={form.nameEn} onChange={set('nameEn')} dir="ltr" /></div>
              <div className="full"><label className="field-label">{t('common.aliases')}</label><input className="field" value={form.aliases} onChange={set('aliases')} /></div>
              <div><label className="field-label">{t('common.kind')}</label><div style={{ marginBottom: 18 }}><Select value={form.stationKind} onChange={(v) => setForm((f) => ({ ...f, stationKind: v }))} ariaLabel={t('common.kind')} options={STATION_KIND_KEYS.map((k) => ({ value: k, label: t(`stationKinds.${k}`) }))} /></div></div>
              <div><label className="field-label">{t('common.governorate')}</label><input className="field" value={form.governorate} onChange={set('governorate')} dir="ltr" /></div>
              <div><label className="field-label">Latitude</label><input className="field" type="number" step="any" required value={form.lat} onChange={set('lat')} dir="ltr" /></div>
              <div><label className="field-label">Longitude</label><input className="field" type="number" step="any" required value={form.lng} onChange={set('lng')} dir="ltr" /></div>
            </div>
            {error && <p className="error" style={{ marginTop: -8 }}>{error}</p>}
            <div className="modal-buttons">
              <button type="button" className="button secondary" onClick={close}>{t('common.cancel')}</button>
              <button type="submit" className="button" disabled={busy}>{modal.id ? t('common.save') : t('common.add')}</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}
