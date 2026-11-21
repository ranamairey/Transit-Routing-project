import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'
import { Api } from '../api/client.js'
import { useAuth } from './AuthContext.jsx'

/**
 * Rider conveniences shared by the planner and the line pages: saved places, recent trips, followed lines,
 * live line status and alerts. Everything works without an account (stored on the device); when signed in,
 * saved places and followed lines are kept on the account so they follow the rider to other devices.
 */
const RiderContext = createContext(null)
const PLACES_KEY = 'sst.places'
const FOLLOWS_KEY = 'sst.follows'
const RECENT_KEY = 'sst.recent'
const SEEN_KEY = 'sst.alertsSeen'
const LIVE_EVERY_MS = 2 * 60 * 1000
const MAX_RECENT = 8

const load = (key, fallback) => { try { return JSON.parse(localStorage.getItem(key) || 'null') ?? fallback } catch { return fallback } }
const store = (key, value) => { try { localStorage.setItem(key, JSON.stringify(value)) } catch { /* ignore */ } }

/** Planner endpoint ⇄ saved place on the account. */
export const toSaved = (endpoint, kind = 'OTHER', label = '') => ({
  id: endpoint.savedId || null, kind, label: label || null, name: endpoint.name,
  stationId: endpoint.mode === 'station' ? endpoint.stationId : null,
  lat: endpoint.mode === 'station' ? null : endpoint.point?.lat, lng: endpoint.mode === 'station' ? null : endpoint.point?.lng,
})
export const toEndpoint = (place, stationsById) => {
  const station = place.stationId ? stationsById?.[place.stationId] : null
  if (place.stationId && station) return { mode: 'station', name: station.name, stationId: station.id, point: station.location, savedId: place.id }
  if (place.lat != null) return { mode: 'pin', name: place.name || place.label, point: { lat: place.lat, lng: place.lng }, savedId: place.id }
  return null
}
const endpointKey = (e) => (e?.mode === 'station' ? `s:${e.stationId}` : e?.point ? `p:${e.point.lat.toFixed(4)},${e.point.lng.toFixed(4)}` : '')
const placeKey = (p) => (p.stationId ? `s:${p.stationId}` : p.lat != null ? `p:${p.lat.toFixed(4)},${p.lng.toFixed(4)}` : '')
const liveTime = (l) => Math.max(Date.parse(l.lastReportAt || 0) || 0, Date.parse(l.statusUpdatedAt || 0) || 0)

export function RiderProvider({ children }) {
  const { user } = useAuth()
  const [places, setPlaces] = useState(() => load(PLACES_KEY, []))
  const [follows, setFollows] = useState(() => new Set(load(FOLLOWS_KEY, [])))
  const [recent, setRecent] = useState(() => load(RECENT_KEY, []))
  const [live, setLive] = useState({})
  const [seenAt, setSeenAt] = useState(() => load(SEEN_KEY, 0))
  const notified = useRef(new Set())

  // On sign-in: the account wins when it has data, otherwise this device's data is uploaded to it.
  useEffect(() => {
    if (!user) return
    let cancelled = false
    ;(async () => {
      try {
        const [serverPlaces, serverFollows] = await Promise.all([Api.account.places(), Api.account.follows()])
        if (cancelled) return
        const localPlaces = load(PLACES_KEY, [])
        if (serverPlaces.length || !localPlaces.length) { setPlaces(serverPlaces); store(PLACES_KEY, serverPlaces) }
        else setPlaces(await Api.account.savePlaces(localPlaces))
        const localFollows = load(FOLLOWS_KEY, [])
        const merged = [...new Set([...serverFollows, ...localFollows])]
        const saved = merged.length !== serverFollows.length ? await Api.account.saveFollows(merged) : serverFollows
        if (!cancelled) { setFollows(new Set(saved)); store(FOLLOWS_KEY, saved) }
      } catch { /* offline: keep device copies */ }
    })()
    return () => { cancelled = true }
  }, [user])

  const savePlaces = useCallback(async (next) => {
    setPlaces(next); store(PLACES_KEY, next)
    if (user) {
      try { const saved = await Api.account.savePlaces(next); setPlaces(saved); store(PLACES_KEY, saved) } catch { /* kept on device */ }
    }
  }, [user])

  const addPlace = useCallback((endpoint, kind, label) => {
    // One Home and one Work; the same spot is never saved twice.
    const key = endpointKey(endpoint)
    const others = places.filter((p) => !(kind !== 'OTHER' && p.kind === kind) && placeKey(p) !== key)
    return savePlaces([...others, { ...toSaved(endpoint, kind, label), id: crypto.randomUUID?.() || String(Date.now()) }])
  }, [places, savePlaces])
  const removePlace = useCallback((id) => savePlaces(places.filter((p) => p.id !== id)), [places, savePlaces])

  const toggleFollow = useCallback(async (routeId) => {
    const next = new Set(follows)
    if (next.has(routeId)) next.delete(routeId); else next.add(routeId)
    setFollows(next); store(FOLLOWS_KEY, [...next])
    if (user) { try { await Api.account.saveFollows([...next]) } catch { /* kept on device */ } }
    return next.has(routeId)
  }, [follows, user])

  const addRecent = useCallback((from, to) => {
    if (!from || !to) return
    const strip = ({ savedId, ...e }) => e // eslint-disable-line no-unused-vars
    const key = `${endpointKey(from)}>${endpointKey(to)}`
    setRecent((prev) => {
      const next = [{ from: strip(from), to: strip(to), at: Date.now() }, ...prev.filter((r) => `${endpointKey(r.from)}>${endpointKey(r.to)}` !== key)].slice(0, MAX_RECENT)
      store(RECENT_KEY, next)
      return next
    })
  }, [])
  const clearRecent = useCallback(() => { setRecent([]); store(RECENT_KEY, []) }, [])

  // Live line status, refreshed every two minutes while the app is open.
  const refreshLive = useCallback(async () => {
    try { setLive(Object.fromEntries((await Api.liveStatus()).map((l) => [l.routeId, l]))) } catch { /* offline */ }
  }, [])
  useEffect(() => {
    refreshLive()
    const id = setInterval(refreshLive, LIVE_EVERY_MS)
    window.addEventListener('online', refreshLive)
    return () => { clearInterval(id); window.removeEventListener('online', refreshLive) }
  }, [refreshLive])

  // Alerts = followed lines that currently have a problem or fresh reports.
  const alerts = useMemo(() => [...follows].map((id) => live[id]).filter(Boolean).sort((a, b) => liveTime(b) - liveTime(a)), [follows, live])
  const unread = alerts.filter((a) => liveTime(a) > seenAt).length
  const markSeen = useCallback(() => { const now = Date.now(); setSeenAt(now); store(SEEN_KEY, now) }, [])

  // While the app is open, show a system notification for a new alert when the rider allowed notifications.
  useEffect(() => {
    if (typeof Notification === 'undefined' || Notification.permission !== 'granted') return
    for (const a of alerts) {
      const key = `${a.routeId}:${liveTime(a)}`
      if (liveTime(a) <= seenAt || notified.current.has(key)) continue
      notified.current.add(key)
      window.dispatchEvent(new CustomEvent('sst:line-alert', { detail: a }))
    }
  }, [alerts, seenAt])

  const value = useMemo(() => ({
    places, addPlace, removePlace, savePlaces,
    follows, toggleFollow, isFollowing: (id) => follows.has(id),
    recent, addRecent, clearRecent,
    live, refreshLive, alerts, unread, markSeen,
  }), [places, addPlace, removePlace, savePlaces, follows, toggleFollow, recent, addRecent, clearRecent, live, refreshLive, alerts, unread, markSeen])

  return <RiderContext.Provider value={value}>{children}</RiderContext.Provider>
}

export function useRider() {
  return useContext(RiderContext)
}
