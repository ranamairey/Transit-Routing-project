import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Api } from '../api/client.js'
import { useAuth } from '../context/AuthContext.jsx'
import { useI18n } from '../i18n/I18nContext.jsx'
import { REPORT_TYPE_KEYS } from '../utils/text.js'
import Select from './Select.jsx'

/** FR-2.1: one-tap report about a line; login required (anti-spam, NFR-3.1). */
export default function ReportDialog({ leg, stations, onClose }) {
  const { user } = useAuth()
  const { t } = useI18n()
  const navigate = useNavigate()
  const [type, setType] = useState('CROWDING')
  const [newFare, setNewFare] = useState('')
  const [stationId, setStationId] = useState('')
  const [description, setDescription] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [done, setDone] = useState(null)

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true); setError('')
    try {
      const body = { reportType: type, routeId: leg.routeId, description: description || null }
      if (type === 'FARE_CHANGE') body.newFare = Number(newFare)
      if (stationId) body.stationId = stationId
      if (navigator.geolocation) {
        await new Promise((resolve) => navigator.geolocation.getCurrentPosition(
          (pos) => { body.location = { lat: pos.coords.latitude, lng: pos.coords.longitude }; resolve() },
          () => resolve(), { timeout: 3000 }))
      }
      setDone(await Api.submitReport(body))
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2 style={{ fontSize: '1.15rem' }}>{t('report.title', { route: leg.routeName })}</h2>
        {!user ? (
          <>
            <p>{t('report.loginNeeded')}</p>
            <div className="row">
              <button className="btn" onClick={() => navigate('/login', { state: { from: '/' } })}>{t('report.login')}</button>
              <button className="btn secondary" onClick={onClose}>{t('report.cancel')}</button>
            </div>
          </>
        ) : done ? (
          <>
            <p className="ok">{t('report.thanks')}</p>
            {done.autoApplied ? <p>{t('report.autoApplied')}</p> : <p className="muted">{t('report.pending')}</p>}
            <button className="btn" onClick={onClose}>{t('report.close')}</button>
          </>
        ) : (
          <form onSubmit={submit}>
            <div className="fieldbox">
              <label>{t('report.type')}</label>
              <div className="chips">
                {REPORT_TYPE_KEYS.map((k) => (
                  <button type="button" key={k} className={type === k ? 'on' : ''} onClick={() => setType(k)}>{t(`reportTypes.${k}`)}</button>
                ))}
              </div>
            </div>
            {type === 'FARE_CHANGE' && (
              <div className="fieldbox">
                <label>{t('report.newFare')}</label>
                <input type="number" min="0" step="50" required value={newFare} onChange={(e) => setNewFare(e.target.value)} />
              </div>
            )}
            <div className="fieldbox">
              <label>{t('report.station')}</label>
              <Select value={stationId} onChange={setStationId} placeholder="—" ariaLabel={t('report.station')}
                options={[{ value: '', label: '—' }, ...stations.map((s) => ({ value: s.id, label: s.name }))]} />
            </div>
            <div className="fieldbox">
              <label>{t('report.details')}</label>
              <textarea rows={2} maxLength={500} value={description} onChange={(e) => setDescription(e.target.value)} />
            </div>
            {error && <p className="error">{error}</p>}
            <div className="row">
              <button type="submit" className="btn" disabled={busy}>{busy ? t('report.submitting') : t('report.submit')}</button>
              <button type="button" className="btn secondary" onClick={onClose}>{t('report.cancel')}</button>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}
