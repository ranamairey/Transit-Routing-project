/**
 * Trip endpoints in URLs, for shareable trip links: "s:<stationId>" for a stop or "<lat>,<lng>" for a map point.
 * Links open the planner with both ends filled in and the search already running.
 */
export function encodeEndpoint(e) {
  if (!e) return ''
  if (e.mode === 'station' && e.stationId) return `s:${e.stationId}`
  if (e.point) return `${e.point.lat.toFixed(5)},${e.point.lng.toFixed(5)}`
  return ''
}

export function decodeEndpoint(value, stationsById, pinLabel) {
  if (!value) return null
  if (value.startsWith('s:')) {
    const s = stationsById[value.slice(2)]
    return s ? { mode: 'station', name: s.name, stationId: s.id, point: s.location } : null
  }
  const [lat, lng] = value.split(',').map(Number)
  if (!Number.isFinite(lat) || !Number.isFinite(lng) || Math.abs(lat) > 90 || Math.abs(lng) > 180) return null
  return { mode: 'pin', name: `${pinLabel} (${lat.toFixed(4)}, ${lng.toFixed(4)})`, point: { lat, lng } }
}

/** Public address of the rider app: the configured site (needed inside the Android app), else this page's address. */
export function appBaseUrl() {
  const configured = import.meta.env.VITE_PUBLIC_URL
  if (configured) return configured.replace(/\/$/, '')
  return (window.location.origin + import.meta.env.BASE_URL).replace(/\/$/, '')
}

export function tripLink(from, to) {
  const q = new URLSearchParams({ from: encodeEndpoint(from), to: encodeEndpoint(to), go: '1' })
  return `${appBaseUrl()}/?${q}`
}

export function metersBetween(a, b) {
  const dLat = (b.lat - a.lat) * 111320
  const dLng = (b.lng - a.lng) * 111320 * Math.cos((a.lat * Math.PI) / 180)
  return Math.sqrt(dLat * dLat + dLng * dLng)
}
