import { useCallback, useEffect, useMemo, useState } from 'react'
import { Api } from '../api/client.js'
import MapView from '../components/MapView.jsx'
import Select from '../components/Select.jsx'
import { Modal, PageHead, Panel, useConfirm, useToast } from '../components/ui.jsx'
import { useNetwork } from '../hooks.js'
import { useI18n } from '../i18n/I18nContext.jsx'
import { STATION_KIND_KEYS } from '../utils/text.js'

const STATUSES = ['PENDING', 'ACCEPTED', 'REJECTED', '']
const BADGE = { PENDING: 'pending', ACCEPTED: 'applied', REJECTED: 'rejected' }

/** Rider corrections to the network data: review on a map, then accept (applies the change) or reject. */
export default function SuggestionsPage() {
  const { t, timeAgo } = useI18n()
  const toast = useToast()
  const confirm = useConfirm()
  const { refresh } = useNetwork()
  const [status, setStatus] = useState('PENDING')
  const [items, setItems] = useState([])
  const [error, setError] = useState('')
  const [open, setOpen] = useState(null)

  const load = useCallback(() => {
    Api.suggestions.list(status || undefined).then(setItems).catch((e) => setError(e.message))
  }, [status])
  useEffect(() => { load() }, [load])

  const reject = async (s) => {
    try { await Api.suggestions.reject(s.id); toast(t('sugg.rejected')); setOpen(null); load() } catch (e) { toast(e.message) }
  }
  const remove = async (s) => {
    if (!(await confirm({ title: t('sugg.remove'), message: t('sugg.confirmRemove'), danger: true, okLabel: t('sugg.remove') }))) return
    try { await Api.suggestions.remove(s.id); setOpen(null); load() } catch (e) { toast(e.message) }
  }

  const subject = (s) => s.type === 'MISSING_PLACE' ? s.name : s.type === 'WRONG_LOCATION' ? s.stationName : s.type === 'WRONG_LINE' ? s.routeName : (s.description || '').slice(0, 60)

  return (
    <div>
      <PageHead eyebrow={t('sugg.eyebrow')} title={t('sugg.title')} description={t('sugg.desc')} />
      {error && <p className="error">{error}</p>}
      <Panel title={t('sugg.title')} count={items.length}
        tools={<div className="chips">{STATUSES.map((k) => <button key={k || 'all'} className={`chip ${status === k ? 'on' : ''}`} onClick={() => setStatus(k)}>{k ? t(`sugg.${k}`) : t('sugg.all')}</button>)}</div>}>
        <div className="table-wrap">
          <table>
            <thead><tr><th>{t('sugg.when')}</th><th>{t('sugg.type')}</th><th>{t('sugg.subject')}</th><th>{t('sugg.by')}</th><th>{t('common.status')}</th><th>{t('common.actions')}</th></tr></thead>
            <tbody>
              {items.map((s) => (
                <tr key={s.id}>
                  <td>{timeAgo(s.createdAt)}</td>
                  <td>{t(`sugg.${s.type}`)}</td>
                  <td><span className="user-name">{subject(s)}</span>{s.description && s.type !== 'OTHER' && <div className="muted" style={{ fontSize: 11 }}>{s.description}</div>}</td>
                  <td className="muted">{s.reporterId}</td>
                  <td><span className={`badge-role ${BADGE[s.status]}`}>{t(`sugg.${s.status}`)}</span></td>
                  <td><button className="button secondary small" onClick={() => setOpen(s)}>{t('sugg.review')}</button></td>
                </tr>
              ))}
              {items.length === 0 && <tr><td colSpan={6} className="empty">{t('sugg.none')}</td></tr>}
            </tbody>
          </table>
        </div>
      </Panel>
      {open && <Review item={open} onClose={() => setOpen(null)} onReject={reject} onRemove={remove}
        onAccepted={() => { toast(t('sugg.accepted')); setOpen(null); load(); refresh?.() }} />}
    </div>
  )
}

function Review({ item, onClose, onReject, onRemove, onAccepted }) {
  const { t } = useI18n()
  const [name, setName] = useState(item.name || '')
  const [nameEn, setNameEn] = useState(item.nameEn || '')
  const [kind, setKind] = useState('POINT')
  const [point, setPoint] = useState(item.location)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const fit = useMemo(() => [item.location, item.stationLocation].filter(Boolean), [item])
  const pending = item.status === 'PENDING'
  const movable = pending && (item.type === 'MISSING_PLACE' || item.type === 'WRONG_LOCATION')

  const accept = async () => {
    setBusy(true); setError('')
    try {
      await Api.suggestions.accept(item.id, item.type === 'MISSING_PLACE'
        ? { name, nameEn: nameEn || null, stationKind: kind, location: point }
        : { location: point })
      onAccepted()
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }

  return (
    <Modal title={t(`sugg.${item.type}`)} subtitle={item.reporterId} onClose={onClose} wide>
      {(item.location || item.stationLocation) && (
        <>
          <div className="map-box" style={{ height: 280 }}>
            <MapView origin={point} destination={item.stationLocation} fit={fit} onClick={movable ? setPoint : undefined} />
          </div>
          <p className="hint">{t('sugg.mapHint')}</p>
        </>
      )}
      {item.type === 'MISSING_PLACE' && pending && (
        <div className="form-grid">
          <div><label className="field-label">{t('common.name')}</label><input className="field" value={name} onChange={(e) => setName(e.target.value)} /></div>
          <div><label className="field-label">{t('sugg.nameEn')}</label><input className="field" dir="ltr" value={nameEn} onChange={(e) => setNameEn(e.target.value)} /></div>
          <div><label className="field-label">{t('common.kind')}</label>
            <Select value={kind} onChange={setKind} ariaLabel={t('common.kind')} options={STATION_KIND_KEYS.map((k) => ({ value: k, label: t(`stationKinds.${k}`) }))} /></div>
        </div>
      )}
      {item.type === 'WRONG_LINE' && <p><b>{item.routeName}</b></p>}
      {item.description && <p>{item.description}</p>}
      {item.type === 'WRONG_LINE' && pending && <p className="hint">{t('sugg.lineHint')}</p>}
      {error && <p className="error">{error}</p>}
      <div className="modal-buttons">
        <button className="button secondary" onClick={() => onRemove(item)}>{t('sugg.remove')}</button>
        {pending && <button className="button secondary" onClick={() => onReject(item)}>{t('sugg.reject')}</button>}
        {pending && (
          <button className="button" disabled={busy || (item.type === 'MISSING_PLACE' && !name.trim())} onClick={accept}>
            {item.type === 'MISSING_PLACE' ? t('sugg.acceptCreate') : item.type === 'WRONG_LOCATION' ? t('sugg.acceptMove') : t('sugg.accept')}
          </button>
        )}
      </div>
    </Modal>
  )
}
