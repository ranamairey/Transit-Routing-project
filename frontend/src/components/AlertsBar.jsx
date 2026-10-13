import { useI18n } from '../i18n/I18nContext.jsx'

/** FR-2.2: recent crowdsourced reports on the suggested lines, plus the general rider notice. */
export default function AlertsBar({ alerts }) {
  const { t, formatFare, timeAgo } = useI18n()
  const live = alerts && alerts.length > 0
  return (
    <section className="alert">
      <div className="symbol">⚠️</div>
      <div>
        <strong>{live ? t('alerts.title') : t('alerts.generalTitle')}</strong>
        {live ? (
          <ul>
            {alerts.map((a) => (
              <li key={a.id}>
                <b>{t(`reportTypes.${a.reportType}`)}</b> {t('alerts.on')} “{a.routeName}”
                {a.newFare != null && <> — {t('alerts.fare')} {formatFare(a.newFare)}</>}
                {a.description && <> — {a.description}</>}
                <span> · {timeAgo(a.sentAt)}{a.applied ? ` · ${t('alerts.applied')}` : ''}</span>
              </li>
            ))}
          </ul>
        ) : <p>{t('alerts.generalBody')}</p>}
      </div>
    </section>
  )
}
