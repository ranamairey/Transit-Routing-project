import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import LiveBadge from '../components/LiveBadge.jsx'
import { useRider } from '../context/RiderContext.jsx'
import { useI18n } from '../i18n/I18nContext.jsx'
import { nameOf } from '../utils/names.js'
import { normalizeArabic, searchStations } from '../utils/text.js'

/** Every servis and bus line with its fare, stop count and live status; search by name, code or a stop on the line. */
export default function LinesPage({ net }) {
  const { t, lang, formatFare } = useI18n()
  const { live, isFollowing, toggleFollow } = useRider()
  const routes = net.network?.routes || []
  const stations = net.network?.stations || []
  const [q, setQ] = useState('')
  const [tab, setTab] = useState('all')

  const list = useMemo(() => {
    let items = routes
    if (tab === 'following') items = items.filter((r) => isFollowing(r.id))
    if (tab === 'live') items = items.filter((r) => live[r.id])
    const query = normalizeArabic(q)
    if (query) {
      const viaStops = new Set(searchStations(stations, q, 40).map((s) => s.id))
      items = items.filter((r) => normalizeArabic(`${r.name} ${r.nameEn || ''} ${r.code || ''}`).includes(query)
        || (r.stationIds || []).some((id) => viaStops.has(id)))
    }
    return [...items].sort((a, b) => (live[b.id] ? 1 : 0) - (live[a.id] ? 1 : 0) || (a.code || a.name).localeCompare(b.code || b.name))
  }, [routes, stations, q, tab, live, isFollowing])

  return (
    <main className="page">
      <section className="panel">
        <div className="panel-head">
          <div><h2>{t('lines.title')}</h2><p className="sub">{t('lines.subtitle')}</p></div>
          <span className="tag">{routes.length}</span>
        </div>
        <div className="lines-tools">
          <input className="search-input" placeholder={t('lines.search')} value={q} onChange={(e) => setQ(e.target.value)} />
          <div className="chips">
            {['all', 'following', 'live'].map((k) => <button key={k} className={tab === k ? 'on' : ''} onClick={() => setTab(k)}>{t(`lines.${k}`)}</button>)}
          </div>
        </div>
      </section>
      <section className="line-grid">
        {list.length === 0 && <p className="muted">{t('lines.none')}</p>}
        {list.map((r) => (
          <article key={r.id} className="line-card">
            <Link to={`/lines/${r.id}`} className="line-main">
              <span className={`veh ${r.vehicleType}`}>{r.vehicleType === 'BUS' ? '🚌' : '🚍'}</span>
              <div>
                <b>{nameOf(r, lang)}</b>
                <small>{r.code && <>{r.code} · </>}{t(`vehicle.${r.vehicleType}`)} · {t('lines.stops', { n: r.stationIds?.length || 0 })} · {formatFare(r.currentFare)}</small>
                <LiveBadge live={live[r.id]} />
              </div>
            </Link>
            <button className={`star ${isFollowing(r.id) ? 'on' : ''}`} title={isFollowing(r.id) ? t('lines.unfollow') : t('lines.follow')}
              aria-pressed={isFollowing(r.id)} onClick={() => toggleFollow(r.id)}>{isFollowing(r.id) ? '★' : '☆'}</button>
          </article>
        ))}
      </section>
    </main>
  )
}
