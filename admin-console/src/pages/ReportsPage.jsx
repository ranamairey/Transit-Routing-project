import { useCallback, useEffect, useState } from 'react'
import { Api } from '../api/client.js'
import MapView from '../components/MapView.jsx'
import Pager from '../components/Pager.jsx'
import { useNetwork } from '../hooks.js'
import { useI18n } from '../i18n/I18nContext.jsx'
import { REPORT_TYPE_KEYS } from '../utils/text.js'

const SIZE = 25

/** FR-3.2: grouped summary, filters, pagination, optional map, approve/reject/delete. */
export default function ReportsPage() {
  const { t, formatFare, timeAgo } = useI18n()
  const { stations, routes, refresh } = useNetwork()
  const [summary, setSummary] = useState([])
  const [pageData, setPageData] = useState({ items: [], total: 0, page: 0 })
  const [page, setPage] = useState(0)
  const [filters, setFilters] = useState({ routeId: '', type: '', applied: '', dismissed: 'false', hours: '168' })
  const [showMap, setShowMap] = useState(false)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setError('')
    try {
      const [s, r] = await Promise.all([Api.reports.summary(), Api.reports.list({ ...filters, page, size: SIZE })])
      setSummary(s); setPageData(r)
    } catch (err) { setError(err.message) }
  }, [filters, page])
  useEffect(() => { load() }, [load])

  const act = async (fn) => { try { await fn(); await load(); await refresh() } catch (err) { setError(err.message) } }
  const set = (k) => (e) => { setFilters((f) => ({ ...f, [k]: e.target.value })); setPage(0) }
  const pins = pageData.items.filter((r) => r.location).map((r) => ({ id: r.id, name: `${t(`reportTypes.${r.reportType}`)} — ${r.routeName}`, location: r.location, stationKind: 'HUB' }))

  return (
    <div>
      <h1 style={{ fontSize: '1.4rem' }}>{t('nav.reports')}</h1>
      {error && <p className="error">{error}</p>}
      <div className="card">
        <h2 style={{ fontSize: '1.05rem' }}>{t('reports.summary')}</h2>
        {summary.length === 0 ? <p className="muted">{t('reports.noRecent')}</p> : (
          <table>
            <thead><tr><th>{t('reports.route')}</th><th>{t('reports.type')}</th><th>{t('reports.count')}</th><th>{t('reports.distinct')}</th><th>{t('reports.pending')}</th><th>{t('reports.routeStatus')}</th><th>{t('reports.latest')}</th></tr></thead>
            <tbody>
              {summary.map((g) => (
                <tr key={g.routeId + g.reportType}>
                  <td><b>{g.routeName}</b></td><td>{t(`reportTypes.${g.reportType}`)}</td><td>{g.reportCount}</td>
                  <td>{g.distinctReporters} / {g.autoApplyThreshold} {g.thresholdReached && <span className="badge CROWDED">{t('reports.threshold')}</span>}</td>
                  <td>{g.pendingCount}</td>
                  <td><span className={`badge ${g.routeStatus}`}>{t(`routeStatus.${g.routeStatus}`)}</span></td>
                  <td>{g.latestAt && timeAgo(g.latestAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="card">
        <div className="row between"><h2 style={{ fontSize: '1.05rem' }}>{t('reports.filters')}</h2>
          <button className="secondary small" onClick={() => setShowMap(!showMap)}>{showMap ? t('reports.hideMap') : t('reports.showMap')}</button></div>
        <div className="row">
          <select style={{ flex: 2 }} value={filters.routeId} onChange={set('routeId')}><option value="">{t('reports.allRoutes')}</option>{routes.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}</select>
          <select style={{ flex: 1 }} value={filters.type} onChange={set('type')}><option value="">{t('reports.allTypes')}</option>{REPORT_TYPE_KEYS.map((k) => <option key={k} value={k}>{t(`reportTypes.${k}`)}</option>)}</select>
          <select style={{ flex: 1 }} value={filters.applied} onChange={set('applied')}><option value="">{t('reports.appliedAny')}</option><option value="true">{t('reports.applied')}</option><option value="false">{t('reports.notApplied')}</option></select>
          <select style={{ flex: 1 }} value={filters.dismissed} onChange={set('dismissed')}><option value="false">{t('reports.notDismissed')}</option><option value="true">{t('reports.dismissed')}</option><option value="">{t('reports.all')}</option></select>
          <select style={{ flex: 1 }} value={filters.hours} onChange={set('hours')}><option value="3">{t('reports.h3')}</option><option value="24">{t('reports.h24')}</option><option value="168">{t('reports.h168')}</option><option value="720">{t('reports.h720')}</option></select>
        </div>
      </div>

      {showMap && <div className="card" style={{ height: 380, padding: 0, overflow: 'hidden' }}><MapView stations={[...stations, ...pins]} routes={routes} /></div>}

      <table>
        <thead><tr><th>{t('reports.time')}</th><th>{t('reports.route')}</th><th>{t('reports.type')}</th><th>{t('reports.details')}</th><th>{t('common.status')}</th><th>{t('common.actions')}</th></tr></thead>
        <tbody>
          {pageData.items.map((r) => (
            <tr key={r.id}>
              <td title={r.sentAt}>{timeAgo(r.sentAt)}</td>
              <td><b>{r.routeName}</b>{r.stationName && <div className="muted" style={{ fontSize: '0.8rem' }}>{t('reports.at')} {r.stationName}</div>}</td>
              <td>{t(`reportTypes.${r.reportType}`)}{r.newFare != null && <div>{formatFare(r.newFare)}</div>}</td>
              <td>{r.description}</td>
              <td>{r.dismissed ? <span className="badge DIVERTED">{t('reports.dismissed')}</span> : r.applied ? <span className="badge FARE_UPDATED">{r.reviewedBy === 'auto' ? t('reports.autoApproved') : t('reports.applied')}</span> : <span className="badge">{t('reports.pendingReview')}</span>}</td>
              <td className="row">
                {!r.applied && !r.dismissed && <button className="small" onClick={() => act(() => Api.reports.approve(r.id))}>{t('reports.approve')}</button>}
                {!r.dismissed && <button className="secondary small" onClick={() => act(() => Api.reports.reject(r.id))}>{t('reports.reject')}</button>}
                <button className="ghost small" style={{ color: 'var(--danger)' }} onClick={() => window.confirm(t('reports.confirmDelete')) && act(() => Api.reports.remove(r.id))}>{t('common.delete')}</button>
              </td>
            </tr>
          ))}
          {pageData.items.length === 0 && <tr><td colSpan={6} className="muted">{t('common.none')}</td></tr>}
        </tbody>
      </table>
      <Pager page={page} size={SIZE} total={pageData.total} onPage={setPage} />
    </div>
  )
}
