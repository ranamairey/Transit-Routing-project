import { useI18n } from '../i18n/I18nContext.jsx'

/** Current status of a line plus a count of fresh rider reports (fades once the alert window passes). */
export default function LiveBadge({ live }) {
  const { t } = useI18n()
  if (!live) return null
  const reports = Object.values(live.reportCounts || {}).reduce((a, b) => a + b, 0)
  return (
    <span className={`live-badge ${live.status !== 'NORMAL' ? 'warn' : ''}`}>
      {live.status !== 'NORMAL' ? t(`routeStatus.${live.status}`) : '⚠'}
      {reports > 0 && <small> · {t('bell.reports', { n: reports })}</small>}
    </span>
  )
}
