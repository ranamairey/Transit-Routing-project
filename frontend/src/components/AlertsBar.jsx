import { useI18n } from '../i18n/I18nContext.jsx'

/** FR-2.2: recent crowdsourced reports on the suggested lines. */
export default function AlertsBar({ alerts }) {
  const { t, formatFare, timeAgo } = useI18n()
  if (!alerts || alerts.length === 0) return null
  return (
    <div className="alerts">
      <h3>{t('alerts.title')}</h3>
      <ul style={{ margin: 0, paddingInlineStart: '1.2rem' }}>
        {alerts.map((a) => (
          <li key={a.id}>
            <b>{t(`reportTypes.${a.reportType}`)}</b> {t('alerts.on')} “{a.routeName}”
            {a.newFare != null && <> — {t('alerts.fare')} {formatFare(a.newFare)}</>}
            {a.description && <> — {a.description}</>}
            <span className="muted"> · {timeAgo(a.sentAt)}{a.applied ? ` · ${t('alerts.applied')}` : ''}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}
