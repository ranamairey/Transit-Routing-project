import { useCallback, useEffect, useState } from 'react'
import { Api } from '../api/client.js'
import MapView from '../components/MapView.jsx'
import { PageHead, Panel, Stat, TableFooter, useConfirm, useToast } from '../components/ui.jsx'
import { useNetwork } from '../hooks.js'
import { useI18n } from '../i18n/I18nContext.jsx'
import { REPORT_TYPE_KEYS } from '../utils/text.js'

const SIZE = 25

export default function ReportsPage() {
  const { t, formatFare, timeAgo } = useI18n()
  const notify = useToast()
  const confirm = useConfirm()
  const { stations, routes, refresh } = useNetwork()
  const [summary, setSummary] = useState([])
  const [stats, setStats] = useState(null)
  const [pageData, setPageData] = useState({ items: [], total: 0 })
  const [page, setPage] = useState(0)
  const [filters, setFilters] = useState({ routeId: '', type: '', applied: '', dismissed: 'false', hours: '168' })
  const [showMap, setShowMap] = useState(false)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setError('')
    try {
      const [s, r, st] = await Promise.all([Api.reports.summary(), Api.reports.list({ ...filters, page, size: SIZE }), Api.stats()])
      setSummary(s); setPageData(r); setStats(st)
    } catch (err) { setError(err.message) }
  }, [filters, page])
  useEffect(() => { load() }, [load])

  const act = async (fn, msg) => { try { await fn(); notify(msg); await load(); await refresh() } catch (err) { notify(err.message, 'error') } }
  const set = (k) => (e) => { setFilters((f) => ({ ...f, [k]: e.target.value })); setPage(0) }
  const pins = pageData.items.filter((r) => r.location).map((r) => ({ id: r.id, name: `${t(`reportTypes.${r.reportType}`)} — ${r.routeName}`, location: r.location, stationKind: 'HUB' }))
  const statusBadge = (r) => r.dismissed ? <span className="badge-role rejected">{t('reports.dismissed')}</span>
    : r.applied ? <span className="badge-role applied">{r.reviewedBy === 'auto' ? t('reports.autoApproved') : t('reports.applied')}</span>
    : <span className="badge-role pending">{t('reports.pendingReview')}</span>

  return (
    <div>
      <PageHead eyebrow={t('reports.eyebrow')} title={t('nav.reports')} description={t('reports.desc')}
        action={<button className="button secondary" onClick={() => setShowMap(!showMap)}>{showMap ? t('reports.hideMap') : t('reports.showMap')}</button>} />
      <section className="stats">
        <Stat label={t('dash.pending')} value={stats?.reportsPending} detail={t('dash.needReview')} icon="!" tone="orange" />
        <Stat label={t('dash.reports24')} value={stats?.reportsLast24h} detail={t('reports.last24')} icon="◫" />
        <Stat label={t('reports.groups')} value={summary.length} detail={t('reports.summary')} icon="≋" tone="mint" />
      </section>
      {error && <p className="error">{error}</p>}

      <Panel title={t('reports.summary')} subtitle={t('reports.allDesc')}>
        <div className="table-wrap">
          <table>
            <thead><tr><th>{t('reports.route')}</th><th>{t('reports.type')}</th><th>{t('reports.count')}</th><th>{t('reports.distinct')}</th><th>{t('reports.pending')}</th><th>{t('reports.routeStatus')}</th><th>{t('reports.latest')}</th></tr></thead>
            <tbody>
              {summary.map((g) => (
                <tr key={g.routeId + g.reportType}>
                  <td><span className="user-name">{g.routeName}</span></td><td>{t(`reportTypes.${g.reportType}`)}</td><td>{g.reportCount}</td>
                  <td>{g.distinctReporters} / {g.autoApplyThreshold} {g.thresholdReached && <span className="badge-role pending">{t('reports.threshold')}</span>}</td>
                  <td>{g.pendingCount}</td><td><span className={`badge-role ${g.routeStatus}`}>{t(`routeStatus.${g.routeStatus}`)}</span></td><td>{g.latestAt && timeAgo(g.latestAt)}</td>
                </tr>
              ))}
              {summary.length === 0 && <tr><td colSpan={7} className="empty">{t('reports.noRecent')}</td></tr>}
            </tbody>
          </table>
        </div>
      </Panel>

      {showMap && <Panel body><div className="map-box"><MapView stations={[...stations, ...pins]} routes={routes} /></div></Panel>}

      <Panel title={t('reports.allTitle')} count={pageData.total} subtitle={t('reports.allDesc')}
        tools={<>
          <select className="filter" value={filters.routeId} onChange={set('routeId')}><option value="">{t('reports.allRoutes')}</option>{routes.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}</select>
          <select className="filter" value={filters.type} onChange={set('type')}><option value="">{t('reports.allTypes')}</option>{REPORT_TYPE_KEYS.map((k) => <option key={k} value={k}>{t(`reportTypes.${k}`)}</option>)}</select>
          <select className="filter" value={filters.applied} onChange={set('applied')}><option value="">{t('reports.appliedAny')}</option><option value="true">{t('reports.applied')}</option><option value="false">{t('reports.notApplied')}</option></select>
          <select className="filter" value={filters.dismissed} onChange={set('dismissed')}><option value="false">{t('reports.notDismissed')}</option><option value="true">{t('reports.dismissed')}</option><option value="">{t('reports.all')}</option></select>
          <select className="filter" value={filters.hours} onChange={set('hours')}><option value="3">{t('reports.h3')}</option><option value="24">{t('reports.h24')}</option><option value="168">{t('reports.h168')}</option><option value="720">{t('reports.h720')}</option></select>
        </>}
        footer={<TableFooter page={page} size={SIZE} total={pageData.total} onPage={setPage} />}>
        <div className="table-wrap">
          <table>
            <thead><tr><th>{t('reports.time')}</th><th>{t('reports.route')}</th><th>{t('reports.type')}</th><th>{t('reports.details')}</th><th>{t('common.status')}</th><th>{t('common.actions')}</th></tr></thead>
            <tbody>
              {pageData.items.map((r) => (
                <tr key={r.id}>
                  <td title={r.sentAt}>{timeAgo(r.sentAt)}</td>
                  <td><div className="user-name">{r.routeName}</div>{r.stationName && <div className="sub">{t('reports.at')} {r.stationName}</div>}</td>
                  <td>{t(`reportTypes.${r.reportType}`)}{r.newFare != null && <div className="sub">{formatFare(r.newFare)}</div>}</td>
                  <td>{r.description}</td>
                  <td>{statusBadge(r)}</td>
                  <td><div className="actions">
                    {!r.applied && !r.dismissed && <button className="action approve" onClick={() => act(() => Api.reports.approve(r.id), t('reports.approved'))}>{t('reports.approve')}</button>}
                    {!r.dismissed && <button className="action" onClick={() => act(() => Api.reports.reject(r.id), t('reports.rejectedMsg'))}>{t('reports.reject')}</button>}
                    <button className="action delete" onClick={async () => (await confirm({ title: t('common.delete'), message: t('reports.confirmDelete'), danger: true, okLabel: t('common.delete') })) && act(() => Api.reports.remove(r.id), t('reports.deletedMsg'))}>{t('common.delete')}</button>
                  </div></td>
                </tr>
              ))}
              {pageData.items.length === 0 && <tr><td colSpan={6} className="empty">{t('common.none')}</td></tr>}
            </tbody>
          </table>
        </div>
      </Panel>
    </div>
  )
}
