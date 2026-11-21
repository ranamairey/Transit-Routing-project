import { useEffect, useState } from 'react'
import { Api } from '../api/client.js'
import { PageHead, Panel, Stat } from '../components/ui.jsx'
import { useI18n } from '../i18n/I18nContext.jsx'

/** What riders search for and where the planner finds no route (gaps that need lines or stops). */
export default function InsightsPage() {
  const { t, timeAgo } = useI18n()
  const [days, setDays] = useState(7)
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => { Api.usage(days).then(setData).catch((e) => setError(e.message)) }, [days])

  const max = Math.max(1, ...(data?.perDay || []).map((d) => d.searches))
  const rate = data && data.searches ? Math.round(((data.searches - data.noResult) / data.searches) * 100) : null

  return (
    <div>
      <PageHead eyebrow={t('usage.eyebrow')} title={t('usage.title')} description={t('usage.desc')}
        action={<div className="chips">{[7, 30, 90].map((d) => <button key={d} className={`chip ${days === d ? 'on' : ''}`} onClick={() => setDays(d)}>{t(`usage.days${d}`)}</button>)}</div>} />
      {error && <p className="error">{error}</p>}
      <section className="stats">
        <Stat label={t('usage.searches')} value={data?.searches} icon="⌕" />
        <Stat label={t('usage.noResult')} value={data?.noResult} icon="!" tone="red" />
        <Stat label={t('usage.rate')} value={rate == null ? '—' : `${rate}%`} icon="✓" tone="mint" />
      </section>
      <Panel title={t('usage.perDay')} body>
        {!data || data.searches === 0 ? <p className="muted">{t('usage.empty')}</p> : (
          <div className="day-bars">
            {data.perDay.map((d) => (
              <div key={d.date} className="day-bar" title={`${d.date}: ${d.searches} / ${d.noResult}`}>
                <div className="col"><span className="ok-part" style={{ height: `${((d.searches - d.noResult) / max) * 100}%` }} /><span className="bad-part" style={{ height: `${(d.noResult / max) * 100}%` }} /></div>
                <small>{d.date.slice(5)}</small>
              </div>
            ))}
          </div>
        )}
      </Panel>
      <div className="two">
        <Panel title={t('usage.gaps')} subtitle={t('usage.gapsHint')} count={data?.topNoResult?.length}>
          <TripTable rows={data?.topNoResult} t={t} timeAgo={timeAgo} />
        </Panel>
        <Panel title={t('usage.topTrips')} count={data?.topTrips?.length}>
          <TripTable rows={data?.topTrips} t={t} timeAgo={timeAgo} />
        </Panel>
      </div>
      <Panel title={t('usage.topDest')} body>
        {!data?.topDestinations?.length ? <p className="muted">{t('usage.empty')}</p> : (
          <div className="bars">
            {data.topDestinations.map((p) => (
              <div className="bar" key={p.name}><span className="label">{p.name}</span><div className="track"><div className="fill" style={{ width: `${(p.count / data.topDestinations[0].count) * 100}%` }} /></div><b>{p.count}</b></div>
            ))}
          </div>
        )}
      </Panel>
    </div>
  )
}

function TripTable({ rows, t, timeAgo }) {
  return (
    <div className="table-wrap">
      <table>
        <thead><tr><th>{t('usage.from')}</th><th>{t('usage.to')}</th><th>{t('usage.count')}</th><th>{t('usage.last')}</th></tr></thead>
        <tbody>
          {(rows || []).map((r, i) => (
            <tr key={i}><td>{r.origin}</td><td>{r.destination}</td><td><b>{r.count}</b></td><td className="muted">{timeAgo(r.lastAt)}</td></tr>
          ))}
          {(!rows || rows.length === 0) && <tr><td colSpan={4} className="empty">{t('usage.empty')}</td></tr>}
        </tbody>
      </table>
    </div>
  )
}
