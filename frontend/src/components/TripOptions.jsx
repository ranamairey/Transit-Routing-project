import { useI18n } from '../i18n/I18nContext.jsx'

function Leg({ leg, onReport }) {
  const { t, formatDuration, formatMeters, formatFare } = useI18n()
  const kind = leg.type === 'WALK' ? 'WALK' : leg.vehicleType
  return (
    <div className="leg">
      <div className={`icon ${kind}`}>{leg.type === 'WALK' ? '🚶' : leg.vehicleType === 'BUS' ? '🚌' : '🚐'}</div>
      <div className="body">
        {leg.type === 'WALK' ? (
          <div className="title">{t('trips.walk', { dist: formatMeters(leg.distanceMeters), to: leg.toName })} <span className="muted">({formatDuration(leg.durationSeconds)})</span></div>
        ) : (
          <>
            <div className="title">
              {t('trips.ride', { vehicle: t(`vehicle.${leg.vehicleType}`), route: leg.routeName })}
              {leg.routeStatus && leg.routeStatus !== 'NORMAL' && <> <span className={`badge ${leg.routeStatus}`}>{t(`routeStatus.${leg.routeStatus}`)}</span></>}
            </div>
            <div>{t('trips.fromTo', { from: leg.fromName, to: leg.toName })}</div>
            <div className="muted">{formatDuration(leg.durationSeconds)} · {formatMeters(leg.distanceMeters)} · {formatFare(leg.fare)}</div>
            {onReport && <button className="ghost small" onClick={(e) => { e.stopPropagation(); onReport(leg) }}>{t('trips.report')}</button>}
          </>
        )}
      </div>
    </div>
  )
}

/** FR-1.3: every option shows total fare, expected time, walking distance and transfers. */
export default function TripOptions({ paths, selected, onSelect, onReport, peak }) {
  const { t, formatDuration, formatMeters, formatFare } = useI18n()
  return (
    <div>
      <div className="row between" style={{ marginBottom: '0.5rem' }}>
        <h2 style={{ fontSize: '1.1rem' }}>{t('trips.title', { n: paths.length })}</h2>
        {peak && <span className="badge CROWDED">{t('trips.peakBadge')}</span>}
      </div>
      {paths.map((p, i) => (
        <div key={i} className={`card clickable ${selected === i ? 'selected' : ''}`} onClick={() => onSelect(i)}>
          <div className="stats">
            <span className="fare">💵 {formatFare(p.totalFare)}</span>
            <span>⏱ {formatDuration(p.totalDurationSeconds)}</span>
            <span>🔁 {p.transfers === 0 ? t('trips.noTransfer') : t('trips.transfers', { n: p.transfers })}</span>
            <span>🚶 {formatMeters(p.walkingMeters)}</span>
          </div>
          {selected === i && p.legs.map((leg, j) => <Leg key={j} leg={leg} onReport={onReport} />)}
        </div>
      ))}
    </div>
  )
}
