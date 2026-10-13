import { useI18n } from '../i18n/I18nContext.jsx'

function Step({ leg, onReport }) {
  const { t, formatDuration, formatMeters, formatFare } = useI18n()
  if (leg.type === 'WALK') {
    return (
      <div className="step">
        <div className="step-icon WALK">🚶</div>
        <div><strong>{t('trips.walk', { dist: formatMeters(leg.distanceMeters), to: leg.toName })}</strong><p>{formatDuration(leg.durationSeconds)}</p></div>
      </div>
    )
  }
  return (
    <div className="step">
      <div className={`step-icon ${leg.vehicleType}`}>{leg.vehicleType === 'BUS' ? '🚌' : '🚍'}</div>
      <div>
        <strong>
          {t('trips.ride', { vehicle: t(`vehicle.${leg.vehicleType}`), route: leg.routeName })}
          {leg.routeStatus && leg.routeStatus !== 'NORMAL' && <span className="status">{t(`routeStatus.${leg.routeStatus}`)}</span>}
        </strong>
        <p>{t('trips.fromTo', { from: leg.fromName, to: leg.toName })} · {formatDuration(leg.durationSeconds)} · {formatMeters(leg.distanceMeters)} · {formatFare(leg.fare)}</p>
        {onReport && <button className="report" onClick={(e) => { e.stopPropagation(); onReport(leg) }}>{t('trips.report')}</button>}
      </div>
    </div>
  )
}

/** FR-1.3: every option shows total fare, expected time, walking distance and transfers. */
export default function TripOptions({ paths, selected, onSelect, onReport, peak, onShow, onShare }) {
  const { t, formatDuration, formatMeters, formatFare } = useI18n()
  return (
    <section className="panel">
      <div className="resultsHead">
        <h3>{t('trips.title')} <span className="tag">{t('trips.count', { n: paths.length })}</span></h3>
        <span className="order">{peak ? t('trips.peakBadge') : t('trips.fastestFirst')}</span>
      </div>
      {paths.map((p, i) => (
        <div key={i} className={`result ${selected === i ? 'selected' : ''}`} onClick={() => onSelect(i)}>
          <div className="result-top">
            <div>
              <div className="price">{formatFare(p.totalFare)} <small>{t('trips.approx')}</small></div>
              <div className="meta">◷ {formatDuration(p.totalDurationSeconds)} &nbsp;·&nbsp; ⇄ {p.transfers === 0 ? t('trips.noTransfer') : t('trips.transfers', { n: p.transfers })} &nbsp;·&nbsp; 🚶 {formatMeters(p.walkingMeters)}</div>
            </div>
            <span className={`pill ${i === 0 ? '' : 'warn'}`}>{i === 0 ? t('trips.best') : t('trips.alternative')}</span>
          </div>
          {selected === i && (
            <>
              {p.legs.map((leg, j) => <Step key={j} leg={leg} onReport={onReport} />)}
              <div className="route-footer">
                <button onClick={(e) => { e.stopPropagation(); onShow?.(i) }}>↗ {t('trips.showOnMap')}</button>
                <button onClick={(e) => { e.stopPropagation(); onShare?.(p) }}>↗ {t('trips.share')}</button>
              </div>
            </>
          )}
        </div>
      ))}
    </section>
  )
}
