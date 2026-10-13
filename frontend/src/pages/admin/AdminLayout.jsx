import { NavLink, Outlet } from 'react-router-dom'
import { useI18n } from '../../i18n/I18nContext.jsx'

/** FR-3: admin dashboard shell. */
export default function AdminLayout() {
  const { t } = useI18n()
  return (
    <div className="page">
      <h1 style={{ fontSize: '1.4rem' }}>{t('admin.title')}</h1>
      <div className="tabs">
        <NavLink to="/admin/reports">{t('admin.reports')}</NavLink>
        <NavLink to="/admin/stations">{t('admin.stations')}</NavLink>
        <NavLink to="/admin/routes">{t('admin.routes')}</NavLink>
      </div>
      <Outlet />
    </div>
  )
}
