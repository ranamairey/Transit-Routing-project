import { useMemo, useState } from 'react'
import { useI18n } from '../i18n/I18nContext.jsx'
import { searchStations } from '../utils/text.js'
import { useToast } from './Toast.jsx'

/**
 * FR-1.1: pick a place by GPS, by dropping a pin on the map, or by smart text search over
 * station names and local aliases (works offline on the cached network).
 */
export default function EndpointPicker({ label, kind, value, onChange, stations, pinMode, onPinMode }) {
  const { t } = useI18n()
  const notify = useToast()
  const [query, setQuery] = useState('')
  const [open, setOpen] = useState(false)
  const [locating, setLocating] = useState(false)

  const suggestions = useMemo(() => searchStations(stations, query), [stations, query])

  const useGps = () => {
    if (!navigator.geolocation) { notify(t('endpoint.noGeo')); return }
    setLocating(true)
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setLocating(false)
        onChange({ mode: 'gps', name: t('endpoint.myLocation'), point: { lat: +pos.coords.latitude.toFixed(6), lng: +pos.coords.longitude.toFixed(6) } })
        notify(t('endpoint.located'))
      },
      () => { setLocating(false); notify(t('endpoint.geoFailed')) },
      { enableHighAccuracy: true, timeout: 10000 },
    )
  }

  return (
    <div className={`field ${pinMode ? 'active' : ''}`}>
      <div className="fieldhead"><span className={`dot ${kind === 'dest' ? 'dest' : ''}`} /> {label}</div>
      {value ? (
        <div className="picked"><span>{value.name}</span><button onClick={() => { onChange(null); setQuery('') }}>{t('endpoint.change')}</button></div>
      ) : (
        <div className="suggest">
          <input
            placeholder={t(kind === 'origin' ? 'endpoint.placeholderFrom' : 'endpoint.placeholderTo')}
            value={query}
            onChange={(e) => { setQuery(e.target.value); setOpen(true) }}
            onFocus={() => setOpen(true)}
            onBlur={() => setTimeout(() => setOpen(false), 150)}
          />
          {open && suggestions.length > 0 && (
            <ul>
              {suggestions.map((s) => (
                <li key={s.id} onMouseDown={() => { onChange({ mode: 'station', name: s.name, stationId: s.id, point: s.location }); setQuery('') }}>
                  {s.name}{s.aliases && <small>{s.aliases}</small>}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
      {!value && (
        <div className="shortcuts">
          {kind === 'origin' && <button onClick={useGps} disabled={locating}>◎ {locating ? t('endpoint.locating') : t('endpoint.gps')}</button>}
          <button className={pinMode ? 'on' : ''} onClick={() => onPinMode(pinMode ? null : kind)}>📍 {pinMode ? t('endpoint.pinMode') : t('endpoint.pin')}</button>
        </div>
      )}
    </div>
  )
}
