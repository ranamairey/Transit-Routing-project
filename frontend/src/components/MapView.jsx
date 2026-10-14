import { useEffect } from 'react'
import { CircleMarker, MapContainer, Marker, Polyline, Popup, TileLayer, Tooltip, ZoomControl, useMap, useMapEvents } from 'react-leaflet'
import L from 'leaflet'
import { useI18n } from '../i18n/I18nContext.jsx'

export const DAMASCUS_CENTER = [33.5102, 36.2913]

// Fallback when the main tile server is busy (503): OSM France Humanitarian style, free and key-less.
const FALLBACK_TILES = ['a', 'b', 'c'].map((s) => `https://${s}.tile.openstreetmap.fr/hot/{z}/{x}/{y}.png`)

/**
 * A failed tile (e.g. 503 from a busy server) is reloaded from the fallback servers, up to three times.
 * Patched at Leaflet's tile level so it covers every tile layer in the app.
 */
if (!L.TileLayer.prototype._fallbackPatched) {
  const original = L.TileLayer.prototype._tileOnError
  L.TileLayer.prototype._tileOnError = function onError(done, tile, e) {
    const m = /\/(\d+)\/(\d+)\/(\d+)\.png/.exec(tile.src || '')
    const tries = Number(tile.dataset.tries || 0)
    if (m && tries < 3) {
      tile.dataset.tries = String(tries + 1)
      const [, z, x, y] = m
      const url = FALLBACK_TILES[(Number(x) + Number(y) + tries) % FALLBACK_TILES.length].replace('{z}', z).replace('{x}', x).replace('{y}', y)
      setTimeout(() => { tile.src = url }, tries === 0 ? 0 : 1500 * tries)
      return
    }
    original.call(this, done, tile, e)
  }
  L.TileLayer.prototype._fallbackPatched = true
}

// Key-free OpenStreetMap tiles by default; set VITE_TILE_URL (and VITE_TILE_URL_DARK) to use MapTiler/CARTO/Stadia with your own key.
// tile.openstreetmap.org without the legacy a/b/c subdomains (those are throttled hardest)
const OSM = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png'
export const BASEMAPS = {
  light: import.meta.env.VITE_TILE_URL || OSM,
  dark: import.meta.env.VITE_TILE_URL_DARK || import.meta.env.VITE_TILE_URL || OSM,
  // when no dedicated dark tiles are configured, the light tiles are inverted with CSS (.map-dark)
  cssDark: !import.meta.env.VITE_TILE_URL_DARK,
}

const pinIcon = (color) => L.divIcon({
  className: '',
  html: `<div style="width:22px;height:22px;border-radius:50% 50% 50% 0;background:${color};transform:rotate(-45deg);border:2px solid #fff;box-shadow:0 1px 4px rgba(0,0,0,.4)"></div>`,
  iconSize: [22, 22],
  iconAnchor: [11, 22],
})
const ORIGIN_ICON = pinIcon('#146c4f')
const DEST_ICON = pinIcon('#b3261e')

function ClickHandler({ onClick }) {
  useMapEvents({ click: (e) => onClick && onClick({ lat: +e.latlng.lat.toFixed(6), lng: +e.latlng.lng.toFixed(6) }) })
  return null
}

function FitBounds({ points }) {
  const map = useMap()
  useEffect(() => {
    if (!points || points.length === 0) return
    if (points.length === 1) { map.setView([points[0].lat, points[0].lng], 14); return }
    map.fitBounds(points.map((p) => [p.lat, p.lng]), { padding: [30, 30] })
  }, [map, points])
  return null
}

const LEG_COLORS = { SERVIS: '#1f5fb3', BUS: '#b3521f', WALK: '#4b5a52' }

/**
 * Lightweight OpenStreetMap map (NFR-1.1). Shows stations, route polylines, origin/destination
 * pins and the legs of the selected trip.
 */
export default function MapView({
  stations = [], routes = [], origin, destination, legs = [], onClick, fit, onStationClick, highlightRouteId, drawing = [], zoomPosition, dimRoutes = false, dark = false,
}) {
  const { t } = useI18n()
  return (
    <MapContainer center={DAMASCUS_CENTER} zoom={12} scrollWheelZoom zoomControl={!zoomPosition} attributionControl={false} style={{ height: '100%', width: '100%' }}>
      {zoomPosition && <ZoomControl position={zoomPosition} />}
      {/* Attribution is shown in the app chrome instead of on the map (OSM requires a visible credit). */}
      <TileLayer key={dark ? 'dark' : 'light'} url={dark ? BASEMAPS.dark : BASEMAPS.light} maxZoom={19} className={dark && BASEMAPS.cssDark ? 'tiles-dark' : 'tiles-light'} />
      <ClickHandler onClick={onClick} />
      <FitBounds points={fit} />

      {routes.map((r) => (
        <Polyline
          key={r.id}
          positions={(r.polyline || []).map((p) => [p.lat, p.lng])}
          pathOptions={{
            color: r.vehicleType === 'BUS' ? LEG_COLORS.BUS : LEG_COLORS.SERVIS,
            weight: highlightRouteId === r.id ? 6 : dimRoutes ? 2 : 3,
            opacity: highlightRouteId && highlightRouteId !== r.id ? 0.25 : dimRoutes ? 0.18 : 0.7,
          }}
        >
          <Tooltip sticky>{r.name}</Tooltip>
        </Polyline>
      ))}

      {stations.map((s) => (
        <CircleMarker
          key={s.id}
          center={[s.location.lat, s.location.lng]}
          radius={s.stationKind === 'STOP' || s.stationKind === 'POINT' ? 5 : 8}
          pathOptions={{ color: '#0b3d2e', fillColor: '#fff', fillOpacity: 1, weight: 2 }}
          eventHandlers={onStationClick ? { click: () => onStationClick(s) } : undefined}
        >
          <Tooltip direction="top">{s.name}</Tooltip>
        </CircleMarker>
      ))}

      {legs.map((leg, i) => (
        <Polyline
          key={i}
          positions={leg.path.map((p) => [p.lat, p.lng])}
          pathOptions={{ color: LEG_COLORS[leg.type === 'WALK' ? 'WALK' : leg.vehicleType] || '#000', weight: 7, opacity: 0.95, dashArray: leg.type === 'WALK' ? '6 8' : null }}
        >
          <Tooltip sticky>{leg.type === 'WALK' ? t('units.walkLabel') : leg.routeName}</Tooltip>
        </Polyline>
      ))}

      {drawing.length > 0 && (
        <Polyline positions={drawing.map((p) => [p.lat, p.lng])} pathOptions={{ color: '#e0a100', weight: 5 }} />
      )}
      {drawing.map((p, i) => (
        <CircleMarker key={`d${i}`} center={[p.lat, p.lng]} radius={4} pathOptions={{ color: '#e0a100', fillOpacity: 1 }} />
      ))}

      {origin && <Marker position={[origin.lat, origin.lng]} icon={ORIGIN_ICON}><Popup>{t('planner.origin')}</Popup></Marker>}
      {destination && <Marker position={[destination.lat, destination.lng]} icon={DEST_ICON}><Popup>{t('planner.destination')}</Popup></Marker>}
    </MapContainer>
  )
}

/** Picks the part of a route polyline that lies between two stops (for drawing a ride leg). */
export function legPath(leg, routesById) {
  if (leg.type === 'WALK' && Array.isArray(leg.path) && leg.path.length >= 2) return leg.path
  const route = leg.routeId ? routesById[leg.routeId] : null
  const line = route?.polyline || []
  if (leg.type === 'RIDE' && line.length >= 2 && leg.from && leg.to) {
    const nearest = (pt) => {
      let best = 0, bestD = Infinity
      line.forEach((p, i) => {
        const d = (p.lat - pt.lat) ** 2 + (p.lng - pt.lng) ** 2
        if (d < bestD) { bestD = d; best = i }
      })
      return best
    }
    let a = nearest(leg.from), b = nearest(leg.to)
    if (a > b) [a, b] = [b, a]
    const slice = line.slice(a, b + 1)
    if (slice.length >= 2) return slice
  }
  return [leg.from, leg.to].filter(Boolean)
}
