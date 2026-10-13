/** Arabic-aware normalisation (mirrors StationSearchService on the backend). */
export function normalizeArabic(text) {
  if (!text) return ''
  return text
    .normalize('NFKC')
    .toLowerCase()
    .trim()
    .replace(/[ً-ْٰـ]/g, '')
    .replace(/[أإآٱ]/g, 'ا')
    .replace(/ة/g, 'ه')
    .replace(/[ىی]/g, 'ي')
    .replace(/[ءئؤ]/g, '')
    .replace(/(^|\s)ال/g, '$1')
    .replace(/[^\p{L}\p{N}\s]/gu, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

export function searchStations(stations, query, limit = 8) {
  const q = normalizeArabic(query)
  if (!q) return []
  const scored = []
  for (const s of stations) {
    const terms = [[normalizeArabic(s.name), 100], ...(s.aliases ? s.aliases.split(/[،,;/]/).map((a) => [normalizeArabic(a), 80]) : [])]
    let best = 0
    for (const [term, base] of terms) {
      if (!term) continue
      if (term === q) best = Math.max(best, base)
      else if (term.startsWith(q)) best = Math.max(best, base - 10)
      else if (term.includes(q)) best = Math.max(best, base - 20)
      else if (q.split(' ').every((w) => term.includes(w))) best = Math.max(best, base - 30)
    }
    if (best > 0) scored.push([best, s])
  }
  return scored.sort((a, b) => b[0] - a[0]).slice(0, limit).map(([, s]) => s)
}

export const REPORT_TYPE_KEYS = ['CROWDING', 'DELAY', 'ROUTE_CHANGE', 'FARE_CHANGE', 'OTHER']
export const ROUTE_STATUS_KEYS = ['NORMAL', 'CROWDED', 'DIVERTED', 'DELAYED', 'FARE_UPDATED']
export const VEHICLE_KEYS = ['SERVIS', 'BUS']
export const STATION_KIND_KEYS = ['STOP', 'POINT', 'TERMINAL', 'TRANSFER_HUB', 'HUB', 'GARAGE']
