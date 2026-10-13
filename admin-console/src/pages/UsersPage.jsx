import { useCallback, useEffect, useState } from 'react'
import { Api } from '../api/client.js'
import Pager from '../components/Pager.jsx'
import { useAuth } from '../context/AuthContext.jsx'
import { useI18n } from '../i18n/I18nContext.jsx'

const SIZE = 25
const EMPTY = { fullName: '', email: '', password: '', role: 'ROLE_USER' }

export default function UsersPage() {
  const { t } = useI18n()
  const { user: me } = useAuth()
  const [data, setData] = useState({ items: [], total: 0 })
  const [page, setPage] = useState(0)
  const [q, setQ] = useState('')
  const [role, setRole] = useState('')
  const [form, setForm] = useState(EMPTY)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const load = useCallback(async () => {
    try { setData(await Api.users.list({ q, role, page, size: SIZE })); setError('') } catch (e) { setError(e.message) }
  }, [q, role, page])
  useEffect(() => { load() }, [load])

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))
  const create = async (e) => {
    e.preventDefault(); setBusy(true); setError('')
    try { await Api.users.create(form); setForm(EMPTY); await load() } catch (err) { setError(err.message) } finally { setBusy(false) }
  }
  const act = async (fn) => { try { await fn(); await load() } catch (err) { setError(err.message) } }
  const changeRole = (u, newRole) => act(() => Api.users.update(u.id, { role: newRole }))
  const resetPassword = (u) => {
    const pw = window.prompt(t('users.newPassword'))
    if (pw) act(() => Api.users.update(u.id, { newPassword: pw }))
  }
  const remove = (u) => window.confirm(t('users.confirmDelete', { email: u.email })) && act(() => Api.users.remove(u.id))

  return (
    <div>
      <h1 style={{ fontSize: '1.4rem' }}>{t('users.title')} <span className="muted" style={{ fontSize: '1rem' }}>({data.total})</span></h1>
      {error && <p className="error">{error}</p>}
      <form className="card" onSubmit={create}>
        <h2 style={{ fontSize: '1.05rem' }}>{t('users.add')}</h2>
        <div className="row">
          <div className="field" style={{ flex: 1 }}><label>{t('users.fullName')}</label><input required value={form.fullName} onChange={set('fullName')} /></div>
          <div className="field" style={{ flex: 1 }}><label>{t('users.email')}</label><input type="email" required value={form.email} onChange={set('email')} dir="ltr" /></div>
          <div className="field" style={{ flex: 1 }}><label>{t('users.password')}</label><input type="password" minLength={8} required value={form.password} onChange={set('password')} /></div>
          <div className="field" style={{ flex: 1 }}><label>{t('users.role')}</label>
            <select value={form.role} onChange={set('role')}><option value="ROLE_USER">{t('users.ROLE_USER')}</option><option value="ROLE_ADMIN">{t('users.ROLE_ADMIN')}</option></select>
          </div>
        </div>
        <button type="submit" disabled={busy}>{t('common.add')}</button>
      </form>
      <div className="row" style={{ marginBottom: '0.5rem' }}>
        <input style={{ flex: 2 }} placeholder={t('common.search')} value={q} onChange={(e) => { setQ(e.target.value); setPage(0) }} />
        <select style={{ flex: 1 }} value={role} onChange={(e) => { setRole(e.target.value); setPage(0) }}>
          <option value="">{t('users.allRoles')}</option><option value="ROLE_USER">{t('users.ROLE_USER')}</option><option value="ROLE_ADMIN">{t('users.ROLE_ADMIN')}</option>
        </select>
      </div>
      <table>
        <thead><tr><th>{t('users.fullName')}</th><th>{t('users.email')}</th><th>{t('users.role')}</th><th>{t('common.actions')}</th></tr></thead>
        <tbody>
          {data.items.map((u) => (
            <tr key={u.id}>
              <td><b>{u.fullName}</b> {u.email === me?.email && <span className="badge">{t('users.you')}</span>}</td>
              <td dir="ltr">{u.email}</td>
              <td>
                <select className="small" value={u.role} disabled={u.email === me?.email} onChange={(e) => changeRole(u, e.target.value)}>
                  <option value="ROLE_USER">{t('users.ROLE_USER')}</option><option value="ROLE_ADMIN">{t('users.ROLE_ADMIN')}</option>
                </select>
              </td>
              <td className="row">
                <button className="ghost small" onClick={() => resetPassword(u)}>{t('users.resetPassword')}</button>
                {u.email !== me?.email && <button className="ghost small" style={{ color: 'var(--danger)' }} onClick={() => remove(u)}>{t('common.delete')}</button>}
              </td>
            </tr>
          ))}
          {data.items.length === 0 && <tr><td colSpan={4} className="muted">{t('common.none')}</td></tr>}
        </tbody>
      </table>
      <Pager page={page} size={SIZE} total={data.total} onPage={setPage} />
    </div>
  )
}
