import { useCallback, useEffect, useState } from 'react'
import { Api } from '../../api/client.js'
import { useI18n } from '../../i18n/I18nContext.jsx'
import { REPORT_TYPE_KEYS } from '../../utils/text.js'

/** FR-3.2: group, filter and review crowdsourced reports; auto-apply status is shown per group. */
export default function AdminReports({ net }) {
  const { t, formatFare, timeAgo } = useI18n()
  const routes = net.network?.routes || []
  const [summary, setSummary] = useState([])
  const [reports, setReports] = useState([])
  const [filters, setFilters] = useState({ routeId: '', type: '', applied: '', dismissed: 'false', hours: '168' })
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setError('')
    try {
      const [s, r] = await Promise.all([Api.admin.summary(), Api.admin.reports(filters)])
      setSummary(s); setReports(r)
    } catch (err) { setError(err.message) }
  }, [filters])
  useEffect(() => { load() }, [load])

  const act = async (fn) => { try { await fn(); await load(); await net.refresh() } catch (err) { setError(err.message) } }
  const set = (k) => (e) => setFilters((f) => ({ ...f, [k]: e.target.value }))

  return (
    <div>
      {error && <p className="error">{error}</p>}
      <div className="card">
        <h2 style={{ fontSize: '1.1rem' }}>{t('admin.summaryTitle')}</h2>
        {summary.length === 0 ? <p className="muted">{t('admin.noRecent')}</p> : (
          <table>
            <thead><tr><th>{t('admin.route')}</th><th>{t('admin.type')}</th><th>{t('admin.count')}</th><th>{t('admin.distinct')}</th><th>{t('admin.pendingCol')}</th><th>{t('admin.routeStatus')}</th><th>{t('admin.latest')}</th></tr></thead>
            <tbody>
              {summary.map((g) => (
                <tr key={g.routeId + g.reportType}>
                  <td><b>{g.routeName}</b></td>
                  <td>{t(`reportTypes.${g.reportType}`)}</td>
                  <td>{g.reportCount}</td>
                  <td>{g.distinctReporters} / {g.autoApplyThreshold} {g.thresholdReached && <span className="badge CROWDED">{t('admin.thresholdReached')}</span>}</td>
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
        <h2 style={{ fontSize: '1.1rem' }}>{t('admin.filterTitle')}</h2>
        <div className="row">
          <select style={{ flex: 2 }} value={filters.routeId} onChange={set('routeId')}>
            <option value="">{t('admin.allRoutes')}</option>
            {routes.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
          </select>
          <select style={{ flex: 1 }} value={filters.type} onChange={set('type')}>
            <option value="">{t('admin.allTypes')}</option>
            {REPORT_TYPE_KEYS.map((k) => <option key={k} value={k}>{t(`reportTypes.${k}`)}</option>)}
          </select>
          <select style={{ flex: 1 }} value={filters.applied} onChange={set('applied')}>
            <option value="">{t('admin.appliedAny')}</option><option value="true">{t('admin.applied')}</option><option value="false">{t('admin.notApplied')}</option>
          </select>
          <select style={{ flex: 1 }} value={filters.dismissed} onChange={set('dismissed')}>
            <option value="false">{t('admin.notDismissed')}</option><option value="true">{t('admin.dismissed')}</option><option value="">{t('admin.all')}</option>
          </select>
          <select style={{ flex: 1 }} value={filters.hours} onChange={set('hours')}>
            <option value="3">{t('admin.h3')}</option><option value="24">{t('admin.h24')}</option><option value="168">{t('admin.h168')}</option><option value="720">{t('admin.h720')}</option>
          </select>
        </div>
      </div>

      <table>
        <thead><tr><th>{t('admin.time')}</th><th>{t('admin.route')}</th><th>{t('admin.type')}</th><th>{t('admin.details')}</th><th>{t('admin.status')}</th><th>{t('admin.actions')}</th></tr></thead>
        <tbody>
          {reports.map((r) => (
            <tr key={r.id}>
              <td title={r.sentAt}>{timeAgo(r.sentAt)}</td>
              <td><b>{r.routeName}</b>{r.stationName && <div className="muted" style={{ fontSize: '0.8rem' }}>{t('admin.at')} {r.stationName}</div>}</td>
              <td>{t(`reportTypes.${r.reportType}`)}{r.newFare != null && <div>{formatFare(r.newFare)}</div>}</td>
              <td>{r.description}</td>
              <td>
                {r.dismissed ? <span className="badge DIVERTED">{t('admin.dismissed')}</span> : r.applied ? <span className="badge FARE_UPDATED">{r.reviewedBy === 'auto' ? t('admin.autoApproved') : t('admin.applied')}</span> : <span className="badge">{t('admin.pendingReview')}</span>}
              </td>
              <td className="row">
                {!r.applied && !r.dismissed && <button className="small" onClick={() => act(() => Api.admin.approve(r.id))}>{t('admin.approve')}</button>}
                {!r.dismissed && <button className="secondary small" onClick={() => act(() => Api.admin.reject(r.id))}>{t('admin.reject')}</button>}
                <button className="ghost small" style={{ color: 'var(--danger)' }} onClick={() => window.confirm(t('admin.confirmDeleteReport')) && act(() => Api.admin.deleteReport(r.id))}>{t('admin.delete')}</button>
              </td>
            </tr>
          ))}
          {reports.length === 0 && <tr><td colSpan={6} className="muted">{t('admin.noMatch')}</td></tr>}
        </tbody>
      </table>
    </div>
  )
}
