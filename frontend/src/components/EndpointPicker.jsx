import { useMemo, useState } from 'react'
import { useI18n } from '../i18n/I18nContext.jsx'
import { searchStations } from '../utils/text.js'

/**
 * FR-1.1: pick a place by GPS, by dropping a pin on the map, or by smart text search over
 * station names and local aliases (works offline on the cached network).
 */
export default function EndpointPicker({ label, kind, value, onChange, stations, pinMode, onPinMode }) {
  const { t } = useI18n()
  const [query, setQuery] = useState('')
  const [open, setOpen] = useState(false)
  const [locating, setLocating] = useState(false)
  const [geoError, setGeoError] = useState('')

  const suggestions = useMemo(() => searchStations(stations, query), [stations, query])

  const useGps = () => {
    if (!navigator.geolocation) { setGeoError(t('endpoint.noGeo')); return }
    setLocating(true); setGeoError('')
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setLocating(false)
        onChange({ mode: 'gps', name: t('endpoint.myLocation'), point: { lat: +pos.coords.latitude.toFixed(6), lng: +pos.coords.longitude.toFixed(6) } })
      },
      () => { setLocating(false); setGeoError(t('endpoint.geoFailed')) },
      { enableHighAccuracy: true, timeout: 10000 },
    )
  }

  return (
    <div className={`endpoint ${kind}`}>
      <label>{label}</label>
      {value ? (
        <div className="row between">
          <span className="picked">{kind === 'origin' ? '🟢' : '🔴'} {value.name}</span>
          <button className="ghost" onClick={() => { onChange(null); setQuery('') }}>{t('endpoint.change')}</button>
        </div>
      ) : (
        <>
          <div className="suggest">
            <input
              placeholder={t('endpoint.placeholder')}
              value={query}
              onChange={(e) => { setQuery(e.target.value); setOpen(true) }}
              onFocus={() => setOpen(true)}
              onBlur={() => setTimeout(() => setOpen(false), 150)}
            />
            {open && suggestions.length > 0 && (
              <ul>
                {suggestions.map((s) => (
                  <li key={s.id} onMouseDown={() => { onChange({ mode: 'station', name: s.name, stationId: s.id, point: s.location }); setQuery('') }}>
                    {s.name}
                    {s.aliases && <small>{s.aliases}</small>}
                  </li>
                ))}
              </ul>
            )}
          </div>
          <div className="row" style={{ marginTop: '0.5rem' }}>
            <button className="secondary small" onClick={useGps} disabled={locating}>{locating ? t('endpoint.locating') : t('endpoint.gps')}</button>
            <button className={`small ${pinMode ? '' : 'secondary'}`} onClick={() => onPinMode(pinMode ? null : kind)}>
              {pinMode ? t('endpoint.pinMode') : t('endpoint.pin')}
            </button>
          </div>
          {geoError && <div className="error">{geoError}</div>}
        </>
      )}
    </div>
  )
}
