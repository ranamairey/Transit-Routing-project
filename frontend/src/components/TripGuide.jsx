import { useEffect, useRef, useState } from 'react'
import { useI18n } from '../i18n/I18nContext.jsx'
import { metersBetween } from '../utils/endpoints.js'

const WALK_DONE_M = 45
const RIDE_DONE_M = 90
const NEXT_STOP_M = 450

/**
 * Live guidance for the chosen trip: follows the rider's position, says which step they are on and how far
 * is left, and warns (vibration + notification) when the stop to get off at is next — servis riders have
 * to ask the driver to stop, so the warning comes early.
 */
export default function TripGuide({ path, onPosition, onClose }) {
  const { t, formatMeters } = useI18n()
  const legs = path.legs
  const [index, setIndex] = useState(0)
  const [pos, setPos] = useState(null)
  const [error, setError] = useState('')
  const [nextStop, setNextStop] = useState(false)
  const warned = useRef(new Set())
  const done = index >= legs.length
  const leg = legs[Math.min(index, legs.length - 1)]

  useEffect(() => {
    if (!navigator.geolocation) { setError(t('guide.noGps')); return undefined }
    const id = navigator.geolocation.watchPosition(
      (p) => {
        const here = { lat: p.coords.latitude, lng: p.coords.longitude, accuracy: p.coords.accuracy }
        setPos(here); setError('')
        onPosition?.(here)
      },
      () => setError(t('guide.noGps')),
      { enableHighAccuracy: true, maximumAge: 5000, timeout: 20000 },
    )
    let lock = null
    navigator.wakeLock?.request('screen').then((l) => { lock = l }).catch(() => {})
    return () => { navigator.geolocation.clearWatch(id); lock?.release?.(); onPosition?.(null) }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const remaining = pos && leg?.to ? metersBetween(pos, leg.to) : null

  useEffect(() => {
    if (done || remaining == null || !leg) return
    const ride = leg.type === 'RIDE'
    if (ride && remaining <= NEXT_STOP_M && !warned.current.has(index)) {
      warned.current.add(index)
      setNextStop(true)
      navigator.vibrate?.([300, 150, 300])
      if (document.visibilityState !== 'visible' && typeof Notification !== 'undefined' && Notification.permission === 'granted') {
        navigator.serviceWorker?.ready.then((reg) => reg.showNotification(t('guide.next'), { body: t('guide.nextBody', { to: leg.toName }), tag: 'trip-guide' })).catch(() => {})
      }
    }
    if (remaining <= (ride ? RIDE_DONE_M : WALK_DONE_M)) {
      setNextStop(false)
      setIndex((i) => i + 1)
      if (index + 1 >= legs.length) navigator.vibrate?.(200)
    }
  }, [remaining, index, leg, done, legs.length, t])

  const instruction = !leg ? '' : leg.type === 'WALK'
    ? t('guide.walkTo', { to: leg.toName })
    : t('guide.rideTo', { route: leg.routeName, to: leg.toName })

  return (
    <div className={`trip-guide ${nextStop ? 'alerting' : ''}`} role="status" aria-live="polite">
      <div className="trip-guide-head">
        <b>{done ? t('guide.arrived') : t('guide.title')}</b>
        <button className="btn secondary small" onClick={onClose}>{t('guide.stop')}</button>
      </div>
      {!done && (
        <>
          <small>{t('guide.leg', { i: index + 1, n: legs.length })}</small>
          <p className="trip-guide-step">{leg.type === 'WALK' ? '🚶' : leg.vehicleType === 'BUS' ? '🚌' : '🚍'} {instruction}</p>
          {nextStop && <p className="trip-guide-alert">🔔 {t('guide.next')} — {t('guide.nextBody', { to: leg.toName })}</p>}
          {leg.type === 'RIDE' && !nextStop && <p className="sub">{t('guide.getOff', { to: leg.toName })}</p>}
          <p className="sub">
            {error || (remaining == null ? t('guide.waiting') : t('guide.left', { dist: formatMeters(remaining) }))}
            {pos?.accuracy > 30 && <> · {t('guide.accuracy', { m: formatMeters(pos.accuracy) })}</>}
          </p>
          <div className="trip-guide-steps">
            {legs.map((l, i) => <span key={i} className={i < index ? 'done' : i === index ? 'now' : ''} />)}
          </div>
        </>
      )}
    </div>
  )
}
