import { useI18n } from '../i18n/I18nContext.jsx'

export default function Pager({ page, size, total, onPage }) {
  const { t } = useI18n()
  const pages = Math.max(1, Math.ceil(total / size))
  return (
    <div className="pager">
      <button className="secondary small" disabled={page <= 0} onClick={() => onPage(page - 1)}>{t('common.prev')}</button>
      <span className="muted">{t('common.page', { p: page + 1, n: pages })} · {t('common.total', { n: total })}</span>
      <button className="secondary small" disabled={page >= pages - 1} onClick={() => onPage(page + 1)}>{t('common.next')}</button>
    </div>
  )
}
