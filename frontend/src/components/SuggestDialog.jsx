import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Api } from '../api/client.js'
import { useAuth } from '../context/AuthContext.jsx'
import { useI18n } from '../i18n/I18nContext.jsx'
import MapView from './MapView.jsx'
import Select from './Select.jsx'

const TYPES = ['MISSING_PLACE', 'WRONG_LOCATION', 'WRONG_LINE', 'OTHER']

/**
 * Riders improve the network data: a missing place (name + map point), a stop drawn in the wrong place,
 * or a line that is wrong. An admin reviews every suggestion before it changes anything.
 */
export default function SuggestDialog({ initial = {}, stations = [], routes = [], onClose }) {
  const { user } = useAuth()
  const { t, dark } = useI18n()
  const navigate = useNavigate()
  const [type, setType] = useState(initial.type || 'MISSING_PLACE')
  const [name, setName] = useState('')
  const [nameEn, setNameEn] = useState('')
  const [stationId, setStationId] = useState(initial.stationId || '')
  const [routeId, setRouteId] = useState(initial.routeId || '')
  const [point, setPoint] = useState(initial.point || null)
  const [description, setDescription] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [done, setDone] = useState(false)

  const station = stations.find((s) => s.id === stationId)
  const needsPoint = type === 'MISSING_PLACE' || type === 'WRONG_LOCATION'
  const fit = useMemo(() => (point ? [point] : station ? [station.location] : null), [point, station])
  const lineStations = useMemo(() => {
    const ids = new Set(routes.flatMap((r) => r.stationIds || []))
    return stations.filter((s) => ids.has(s.id))
  }, [routes, stations])

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true); setError('')
    try {
      await Api.suggest({
        type, name: name || null, nameEn: nameEn || null, description: description || null,
        location: needsPoint ? point : null,
        stationId: type === 'WRONG_LOCATION' ? stationId || null : null,
        routeId: type === 'WRONG_LINE' ? routeId || null : null,
      })
      setDone(true)
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal wide" onClick={(e) => e.stopPropagation()}>
        <h2 style={{ fontSize: '1.15rem' }}>{t('suggest.title')}</h2>
        {!user ? (
          <>
            <p>{t('suggest.loginNeeded')}</p>
            <div className="row">
              <button className="btn" onClick={() => navigate('/login')}>{t('report.login')}</button>
              <button className="btn secondary" onClick={onClose}>{t('report.cancel')}</button>
            </div>
          </>
        ) : done ? (
          <>
            <p className="ok">{t('suggest.thanks')}</p>
            <button className="btn" onClick={onClose}>{t('report.close')}</button>
          </>
        ) : (
          <form onSubmit={submit}>
            <div className="fieldbox">
              <label>{t('suggest.type')}</label>
              <div className="chips">
                {TYPES.map((k) => <button type="button" key={k} className={type === k ? 'on' : ''} onClick={() => setType(k)}>{t(`suggest.${k}`)}</button>)}
              </div>
            </div>
            {type === 'MISSING_PLACE' && (
              <div className="two-cols">
                <div className="fieldbox"><label>{t('suggest.name')}</label><input required maxLength={120} value={name} onChange={(e) => setName(e.target.value)} /></div>
                <div className="fieldbox"><label>{t('suggest.nameEn')}</label><input maxLength={120} dir="ltr" value={nameEn} onChange={(e) => setNameEn(e.target.value)} /></div>
              </div>
            )}
            {type === 'WRONG_LOCATION' && (
              <div className="fieldbox">
                <label>{t('suggest.stop')}</label>
                <Select value={stationId} onChange={(v) => { setStationId(v); setPoint(null) }} ariaLabel={t('suggest.stop')}
                  options={[{ value: '', label: '—' }, ...lineStations.map((s) => ({ value: s.id, label: s.name }))]} />
              </div>
            )}
            {type === 'WRONG_LINE' && (
              <div className="fieldbox">
                <label>{t('suggest.line')}</label>
                <Select value={routeId} onChange={setRouteId} ariaLabel={t('suggest.line')}
                  options={[{ value: '', label: '—' }, ...routes.map((r) => ({ value: r.id, label: r.code ? `${r.code} · ${r.name}` : r.name }))]} />
              </div>
            )}
            {needsPoint && (
              <div className="fieldbox">
                <label>{point ? `✓ ${t('suggest.picked')}` : t('suggest.pick')}</label>
                <div className="mini-map">
                  <MapView dark={dark} stations={station ? [station] : []} origin={point} fit={fit} onClick={setPoint} />
                </div>
              </div>
            )}
            <div className="fieldbox">
              <label>{t('suggest.details')}</label>
              <textarea rows={2} maxLength={600} placeholder={t('suggest.detailsHint')} value={description} onChange={(e) => setDescription(e.target.value)}
                required={type === 'WRONG_LINE' || type === 'OTHER'} />
            </div>
            {error && <p className="error">{error}</p>}
            <div className="row">
              <button type="submit" className="btn" disabled={busy || (needsPoint && !point) || (type === 'WRONG_LOCATION' && !stationId) || (type === 'WRONG_LINE' && !routeId)}>
                {busy ? t('suggest.sending') : t('suggest.send')}
              </button>
              <button type="button" className="btn secondary" onClick={onClose}>{t('report.cancel')}</button>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}
